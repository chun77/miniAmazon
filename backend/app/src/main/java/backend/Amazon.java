package backend;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import backend.protocol.AmazonUps.AUCommands;
import backend.protocol.AmazonUps.AUConfirmConnect;
import backend.protocol.AmazonUps.AUNeedATruck;
import backend.protocol.AmazonUps.AUTruckCanGo;
import backend.protocol.AmazonUps.Err;
import backend.protocol.AmazonUps.UACommands;
import backend.protocol.AmazonUps.UADelivered;
import backend.protocol.AmazonUps.UAInitConnect;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.protocol.AmazonUps.Pack;
import backend.protocol.AmazonUps.Product;
import backend.protocol.WorldAmazon.*;
import backend.utils.DBCtrler;
import backend.utils.ProductToAProduct;
import backend.utils.Recver;
import backend.utils.Sender;

public class Amazon {
    private static final int FRONTEND_SERVER_PORT = 8888; 
    private static final int UPS_SERVER_PORT = 9999; 
    private static final int THREAD_POOL_SIZE = 10;
    private static final String UPS_REMOTE_IP = "vcm-38153.vm.duke.edu";
    private static final int UPS_REMOTE_PORT = 9999;

    private ExecutorService threadPool;
    private WorldComm worldComm;
    private UPSComm upsComm;
    private FrontendComm frontendComm;
    private DBCtrler dbCtrler;

    private static long seqnum;
    private final List<WareHouse> whs;
    private List<Package> unfinishedPackages;
    private Map<Long, Timer> unackedMsgsTimer;
    // fields for communication with world
    private InputStream worldRecver;
    private OutputStream worldSender;
    private InputStream upsRecver;
    private OutputStream upsSender;

    public Amazon() {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        worldComm = new WorldComm();
        upsComm = new UPSComm();
        frontendComm = new FrontendComm();
        dbCtrler = new DBCtrler();
        seqnum = 0;
        whs = new ArrayList<>();
        unfinishedPackages = new ArrayList<>();
        unackedMsgsTimer = new HashMap<>();
    }

    public void startWorldRecver() {
        // start a thread to receive message from world
        Thread worldRecverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    AResponses res = worldComm.RecvOneRspsFromWorld(worldRecver, worldSender);
                    // use threadpool to handle the message from world
                    threadPool.execute(() -> {
                        processWorldMsgs(res);
                    });
                }
            }
        });
        worldRecverThread.start();
    }

    // public void startUpsServer() {
    //     // start a thread to receive message from ups
    //     Thread upsServerThread = new Thread(new Runnable() {
    //         @Override
    //         public void run() {
    //             try (ServerSocket serverSocket = new ServerSocket(UPS_SERVER_PORT);) {
    //                 while (true) {
    //                     Socket clientSocket = serverSocket.accept(); 
    //                     // use threadpool to handle the message from ups
    //                     threadPool.execute(() -> {
    //                         UACommands cmds = upsComm.recvOneCmdsFromUps(clientSocket);
    //                         processUpsMsgs(cmds);
    //                     });
    //                 }
    //             } catch (IOException e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //     });
    //     upsServerThread.start();
    // }

    public void startUpsServer() {
        // start a thread to receive message from ups
        Thread upsServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    UACommands cmds = upsComm.recvOneCmdsFromUps(upsRecver, upsSender);
                    // use threadpool to handle the message from ups
                    threadPool.execute(() -> {
                        processUpsMsgs(cmds);
                    });
                }
            }
        });
        upsServerThread.start();
    }

    public void startFrontendServer() {
        // start a thread to receive message from frontend
        Thread frontendServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket(FRONTEND_SERVER_PORT);) {
                    while (true) {
                        Socket clientSocket = serverSocket.accept(); 
                        // use threadpool to handle the message from frontend
                        threadPool.execute(() -> {
                            long package_id = frontendComm.recvOneOrderFromFrontend(clientSocket);
                            System.out.println("get package_id from db: " + package_id);
                            Package pkg = dbCtrler.getPackageByID(package_id);
                            System.out.println("get package from db: " + pkg.toString());
                            synchronized (unfinishedPackages) {
                                unfinishedPackages.add(pkg);
                            }
                            // send to world to purchase more
                            sendToPurchase(pkg);
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        frontendServerThread.start();
    }

    public void initialize() {
        initializeWHs();
        long worldIDFromUps = recvWorldID();
        while(true) {
            try{
                // only for test
                //Socket worldSocket = worldComm.connectToworldWithoudID(whs);
                Socket worldSocket = worldComm.connectToWorld(worldIDFromUps, whs);
                System.out.println("connected to world");
                if(worldSocket != null) {
                    worldRecver = worldSocket.getInputStream();
                    worldSender = worldSocket.getOutputStream();
                    break;
                }
            } catch (Exception e) {
                // should not reloop like this?
                System.out.println("Failed to connect to the world, try again");
                e.printStackTrace();
                continue;
            }
        }
    }

    public long recvWorldID() {
        try (ServerSocket serverSocket = new ServerSocket(9999);) {
            System.out.println("waiting for worldID");
            Socket clientSocket = serverSocket.accept(); 
            upsRecver = clientSocket.getInputStream();
            upsSender = clientSocket.getOutputStream();
            UAInitConnect.Builder msgB = UAInitConnect.newBuilder();
            
            Recver.recvMsgFrom(msgB, upsRecver);
            long worldID = msgB.getWorldid();
            sendBackConnected(worldID, upsSender);
            System.out.println("received worldID: " + worldID);
            return worldID;
        } catch (IOException e) {
            return recvWorldID();
        }
    }

    public void sendBackConnected(long worldID, OutputStream out) {
        AUConfirmConnect.Builder msg = AUConfirmConnect.newBuilder();
        msg.setWorldid(worldID);
        msg.setConnected(true);
        Sender.sendMsgTo(msg.build(), out);
    }

    public static long getSeqnum() {
        long tmp = seqnum;
        seqnum++;
        return tmp;
    }

    public InputStream getWorldRecver() {
        return worldRecver;
    }

    public OutputStream getWorldSender() {
        return worldSender;
    }

    // this is for test
    private void initializeWHs() {
        WareHouse wh1 = new WareHouse(1, new Location(0, 0));
        WareHouse wh2 = new WareHouse(2, new Location(0, 10));
        WareHouse wh3 = new WareHouse(3, new Location(10, 0));
        WareHouse wh4 = new WareHouse(4, new Location(10, 10));
        WareHouse wh5 = new WareHouse(5, new Location(20, 10));
        WareHouse wh6 = new WareHouse(6, new Location(10, 20));
        WareHouse wh7 = new WareHouse(7, new Location(20, 20));
        WareHouse wh8 = new WareHouse(8, new Location(30, 20));
        WareHouse wh9 = new WareHouse(9, new Location(20, 30));
        WareHouse wh10 = new WareHouse(10, new Location(30, 30));
        whs.add(wh1);
        whs.add(wh2);
        whs.add(wh3);
        whs.add(wh4);
        whs.add(wh5);
        whs.add(wh6);
        whs.add(wh7);
        whs.add(wh8);
        whs.add(wh9);
        whs.add(wh10);
    }

    // Below are the methods to process the responses from the world
    // The responses include: APurchaseMore, APacked, ALoaded, AErr, APackage
    // processWorldMsgs is the main method to process the responses
    // processArrived, processReady, processLoaded, processErr, processPackageStatus 
    // are the helper methods to process the responses

    public void processWorldMsgs(AResponses reps) {
        for (APurchaseMore arrived : reps.getArrivedList()) {
            processArrived(arrived);
        }
        for (APacked ready : reps.getReadyList()) {
            processReady(ready);
        }
        for (ALoaded loaded : reps.getLoadedList()) {
            processLoaded(loaded);
        }
        for (AErr err : reps.getErrorList()) {
            processErr(err);
        }
        for (Long ack : reps.getAcksList()) {
            Timer timer = unackedMsgsTimer.get(ack);
            if (timer != null) {
                timer.cancel();
                unackedMsgsTimer.remove(ack);
            }
        }
        for (APackage packageStatus : reps.getPackagestatusList()) {
            processPackageStatus(packageStatus);
        }
    }

    private void processArrived(APurchaseMore arrived) {
        synchronized (unfinishedPackages) {
            System.out.println("unfinishedPackages: " + unfinishedPackages.toString());
            for(Package p: unfinishedPackages){
                if(p.getWh().getId() == arrived.getWhnum() && ProductToAProduct.hasSameProducts(arrived.getThingsList(), p.getProducts())){
                    System.out.println("Package " + p.getPackageID() + " has arrived");
                    p.setStatus("PACKING");
                    dbCtrler.updatePackageStatus(p.getPackageID(), "PACKING");
                    sendToPack(p);
                    sendNeedATruck(p);
                    break;
                }
            }
        }
    }

    private void processReady(APacked ready) {
        // if the truck has arrived, send load to world
        // 1. get the package
        synchronized (unfinishedPackages) {
            for(Package p: unfinishedPackages){
                if(p.getPackageID() == ready.getShipid()){
                    // set status to PACKED
                    p.setStatus("PACKED");
                    dbCtrler.updatePackageStatus(p.getPackageID(), "PACKED");
                    System.out.println("truckid: " + p.getTruckID());
                    // if the truck has arrived, send load to world
                    if(p.getTruckID() != -1){
                        p.setStatus("LOADING");
                        dbCtrler.updatePackageStatus(p.getPackageID(), "LOADING");
                        sendToLoad(p);
                    }
                    break;
                }
            }
        }
    }

    private void processLoaded(ALoaded loaded) {
        // 1. get the package
        synchronized (unfinishedPackages) {
            for(Package p: unfinishedPackages){
                if(p.getPackageID() == loaded.getShipid()){
                    p.setStatus("LOADED");
                    dbCtrler.updatePackageStatus(p.getPackageID(), "LOADED");
                    sendTruckCanGo(p);
                    p.setStatus("DELIVERING");
                    dbCtrler.updatePackageStatus(p.getPackageID(), "DELIVERING");
                    break;
                }
            }
        }
    }

    private void processErr(AErr err) {
        System.out.println("Error: " + err.toString());
    }

    private void processPackageStatus(APackage packageStatus) {
        System.out.println("Package status: " + packageStatus.toString());
    }

    // Methods processing the responses from the world over

    // Below are the methods to send commands to the world

    public void sendOneCmdsToWorld(ACommands cmds, Long seqnum, OutputStream out) throws UnknownHostException, IOException {
        // send commands to the world
        // use Timer, if no acks received in 5 seconds, resend the commands
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (out) {
                    Sender.sendMsgTo(cmds, out);
                }
            }
        }, 0, 5000);
        unackedMsgsTimer.put(seqnum, timer);
        // // receive the response from the world
        // AResponses.Builder responsesB = AResponses.newBuilder();
        // synchronized (in) {
        //     Recver.recvMsgFrom(responsesB, in);
        // }
        // AResponses responses = responsesB.build();
        // // check if the received acks match the seqnums
        // if (checkAcks(responses, seqnums)) {
        //     System.out.println("Received acks match the seqnums.");
        // } else {
        //     System.out.println("Received acks do not match the seqnums. Resending commands...");
        //     // resend the commands
        //     synchronized (out) {
        //         Sender.sendMsgTo(cmds, out);
        //     }
        // }
    }

    public void sendToPurchase(Package pkg){
        int whnum = pkg.getWh().getId();
        List<AProduct> things = ProductToAProduct.genAProductList(pkg.getProducts());
        long seqnum = getSeqnum();
        WorldMsger msger = new WorldMsger();
        msger.purchaseMore(whnum, things, seqnum);
        try {
            sendOneCmdsToWorld(msger.getCommands(), seqnum, worldSender);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendToPack(Package pkg){
        int whnum = pkg.getWh().getId();
        List<AProduct> things = ProductToAProduct.genAProductList(pkg.getProducts());
        long shipid = pkg.getPackageID();
        long seqnum = getSeqnum();
        WorldMsger msger = new WorldMsger();
        msger.pack(whnum, things, shipid, seqnum);
        try {
            sendOneCmdsToWorld(msger.getCommands(), seqnum, worldSender);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendToLoad(Package pkg){
        int whnum = pkg.getWh().getId();
        int truckid = pkg.getTruckID();
        long shipid = pkg.getPackageID();
        long seqnum = Amazon.getSeqnum();
        WorldMsger msger = new WorldMsger();
        msger.load(whnum, truckid, shipid, seqnum);
        try {
            sendOneCmdsToWorld(msger.getCommands(), seqnum, worldSender);
        } catch (UnknownHostException e) {
            // TODO: how to solve this exception?
            e.printStackTrace();
        } catch (IOException e) {
            // TODO: how to solve this exception?
            e.printStackTrace();
        }
    }

    // Methods sending commands to the world over

    // Below are the methods to process the commands from UPS

    public void processUpsMsgs(UACommands cmds) {
        for(UATruckArrived cmd : cmds.getArrivedList()) {
            processArrived(cmd);
        }
        for(UADelivered cmd : cmds.getDeliveredList()) {
            processDelivered(cmd);
        }
        for(Err err : cmds.getErrorsList()) {
            processErrors(err);
        }
        for(Long ack : cmds.getAcksList()) {
            System.out.println("Received ack from UPS: " + ack);
            Timer timer = unackedMsgsTimer.get(ack);
            if (timer != null) {
                timer.cancel();
                unackedMsgsTimer.remove(ack);
            }
        }
    }

    private void processArrived(UATruckArrived arrived) {
        int truckID = arrived.getTruckid();
        synchronized(unfinishedPackages) {
            for(Package p : unfinishedPackages) {
                if(p.getTrackingID().equals(arrived.getTrackingid())){
                    p.setTruckID(truckID);
                    // if this package is packed, tell world to load
                    if(p.getStatus() == "PACKED"){
                        p.setStatus("LOADING");
                        dbCtrler.updatePackageStatus(p.getPackageID(), "LOADING");
                        sendToLoad(p);
                    }
                    break;
                }
            }
        }
    }

    private void processDelivered(UADelivered delivered) {
        synchronized(unfinishedPackages) {
            for(Package p : unfinishedPackages) {
                if(p.getTrackingID().equals(delivered.getTrackingid())){
                    p.setStatus("DELIVERED");
                    dbCtrler.updatePackageStatus(p.getPackageID(), "DELIVERED");
                    unfinishedPackages.remove(p);
                    break;
                }
            }
        }
    }

    private void processErrors(Err err) {
        System.out.println("UPS error: " + err.getMsg());
    }

    // Methods processing the commands from UPS over

    // Below are the methods to send commands to UPS

    // public void sendOneCmdsToUps(AUCommands cmds, Long seqnums) {
    //     // send commands to UPS
    //     // use Timer, if no acks received in 5 seconds, resend the commands
    //     Timer timer = new Timer();
    //     unackedMsgsTimer.put(seqnums, timer);
    //     timer.schedule(new TimerTask() {
    //         @Override
    //         public void run() {
    //             try (Socket socket = new Socket(UPS_REMOTE_IP, UPS_REMOTE_PORT)) {
    //                 OutputStream out = socket.getOutputStream();
    //                 System.out.println("in sendOneCmdsToUPS");
    //                 Sender.sendMsgTo(cmds, out);
    //             } catch (IOException e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //     }, 5000);
        
    // }

    public void sendOneCmdsToUps(AUCommands cmds, Long seqnum) {
        // send commands to UPS
        // use Timer, if no acks received in 5 seconds, resend the commands
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Sender.sendMsgTo(cmds, upsSender);
            }
        }, 0, 5000);
        unackedMsgsTimer.put(seqnum, timer);
    }

    public void sendNeedATruck(Package pkg){
        int dest_x = pkg.getDest().getXLocation();
        int dest_y = pkg.getDest().getYLocation();
        long seqnum = getSeqnum();
        // generate List<AProduct> products
        List<Product> products = pkg.getProducts();
        // generate Pack
        int wh_id = pkg.getWh().getId();
        String tracking_id = pkg.getTrackingID();
        int amazonAccount = pkg.getAmazonAccount();
        long package_id = pkg.getPackageID();
        Pack pack = Pack.newBuilder().setWhId(wh_id).addAllThings(products).setTrackingid(tracking_id).setPackageid(package_id).setAmazonaccount(amazonAccount).setDestX(dest_x).setDestY(dest_y).build();
        // what about ups account?
        AUNeedATruck needATruck = AUNeedATruck.newBuilder().setPack(pack).setSeqnum(seqnum).build();
        AUCommands.Builder cmds = AUCommands.newBuilder().addNeed(needATruck);
        sendOneCmdsToUps(cmds.build(), seqnum);
    }

    public void sendTruckCanGo(Package pkg) {
        int truck_id = pkg.getTruckID();
        long seqnum = getSeqnum();
        // generate ATruckCanGo
        AUTruckCanGo truckCanGo = AUTruckCanGo.newBuilder().setTruckid(truck_id).setSeqnum(seqnum).build();
        AUCommands.Builder cmds = AUCommands.newBuilder().addGo(truckCanGo);
        sendOneCmdsToUps(cmds.build(), seqnum);
    }

    // Methods sending commands to UPS over


}
