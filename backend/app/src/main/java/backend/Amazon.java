package backend;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import backend.protocol.AmazonUps.AUCommands;
import backend.protocol.AmazonUps.AUNeedATruck;
import backend.protocol.AmazonUps.AUTruckCanGo;
import backend.protocol.AmazonUps.Err;
import backend.protocol.AmazonUps.UACommands;
import backend.protocol.AmazonUps.UADelivered;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.protocol.AmazonUps.Pack;
import backend.protocol.WorldAmazon.*;
import backend.utils.DBCtrler;
import backend.utils.Sender;

public class Amazon {
    private static final int FRONTEND_SERVER_PORT = 8888; 
    private static final int UPS_SERVER_PORT = 9999; 
    private static final int THREAD_POOL_SIZE = 10;

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

    public void startUpsServer() {
        // start a thread to receive message from ups
        Thread upsServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket(UPS_SERVER_PORT);) {
                    while (true) {
                        Socket clientSocket = serverSocket.accept(); 
                        // use threadpool to handle the message from ups
                        threadPool.execute(() -> {
                            UACommands cmds = upsComm.recvOneCmdsFromUps(clientSocket);
                            processUpsMsgs(cmds);
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
                            Package pkg = dbCtrler.getPackageByID(package_id);
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
        //long worldIDFromUps = upsComm.recvWorldID();
        while(true) {
            try{
                // only for test
                System.out.println("try to connect to world");
                Socket worldSocket = worldComm.connectToworldWithoudID(whs);
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
        WareHouse wh1 = new WareHouse(1, new Location(10, 10));
        WareHouse wh2 = new WareHouse(2, new Location(20, 20));
        WareHouse wh3 = new WareHouse(3, new Location(30, 30));
        WareHouse wh4 = new WareHouse(4, new Location(40, 40));
        whs.add(wh1);
        whs.add(wh2);
        whs.add(wh3);
        whs.add(wh4);
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
            for(Package p: unfinishedPackages){
                if(p.getWh().getId() == arrived.getWhnum() && p.getProducts() == arrived.getThingsList()){
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
        unackedMsgsTimer.put(seqnum, timer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (out) {
                    Sender.sendMsgTo(cmds, out);
                }
            }
        }, 5000);
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
        List<AProduct> things = pkg.getProducts();
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
        List<AProduct> things = pkg.getProducts();
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
                if(p.getPackageID() == arrived.getPackageid()){
                    p.setTruckID(truckID);
                    // if this package is packed, tell world to load
                    if(p.getStatus() == "PACKED"){
                        p.setStatus("LOADING");
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
                if(p.getPackageID() == delivered.getPackageid()){
                    p.setStatus("DELIVERED");
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

    public void sendOneCmdsToUps(AUCommands cmds, Long seqnums) {
        // send commands to UPS
        // use Timer, if no acks received in 5 seconds, resend the commands
        Timer timer = new Timer();
        unackedMsgsTimer.put(seqnums, timer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try (Socket socket = new Socket("ups_ip", 9998)) {
                    OutputStream out = socket.getOutputStream();
                    Sender.sendMsgTo(cmds, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 5000);
        
    }

    public void sendNeedATruck(Package pkg){
        int wh_x = pkg.getWh().getLocation().getXLocation();
        int wh_y = pkg.getWh().getLocation().getYLocation();
        int dest_x = pkg.getDest().getXLocation();
        int dest_y = pkg.getDest().getYLocation();
        long seqnum = getSeqnum();
        // generate List<AProduct> products
        List<AProduct> products = pkg.getProducts();
        // generate APack
        int whnum = pkg.getWh().getId();
        long package_id = pkg.getPackageID();
        long tracking_id = pkg.getTrackingID();
        Pack pack = Pack.newBuilder().setWhnum(whnum).addAllThings(products).setPackageid(package_id).setTrackingid(tracking_id).build();
        AUNeedATruck needATruck = AUNeedATruck.newBuilder().setWhX(wh_x).setWhY(wh_y).setDestX(dest_x).setDestY(dest_y).setPack(pack).build();
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
