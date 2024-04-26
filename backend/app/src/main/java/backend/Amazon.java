package backend;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;

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
import backend.protocol.AmazonUps.UAChangeAddr;
import backend.protocol.WorldAmazon.*;
import backend.utils.DBCtrler;
import backend.utils.EmailSender;
import backend.utils.ProductToAProduct;
import backend.utils.Recver;
import backend.utils.Sender;
import backend.utils.SlidingWindow;

public class Amazon {
    private static final int FRONTEND_SERVER_PORT = 8888; 
    private static final int UPS_SERVER_PORT = 9999; 
    private static final int THREAD_POOL_SIZE = 20;

    private ExecutorService threadPool;
    private WorldComm worldComm;
    private UPSComm upsComm;
    private FrontendComm frontendComm;
    private DBCtrler dbCtrler;
    private EmailSender emailSender;

    private static long seqnum;
    private final List<WareHouse> whs;
    private List<Package> unfinishedPackages;
    private Map<Long, Timer> unackedMsgsTimer;
    private SlidingWindow recvedSeqFromWorld;
    private SlidingWindow recvedSeqFromUps;
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
        emailSender = new EmailSender();
        seqnum = 0;
        whs = new ArrayList<>();
        unfinishedPackages = new ArrayList<>();
        unackedMsgsTimer = new HashMap<>();
        recvedSeqFromWorld = new SlidingWindow(20);
        recvedSeqFromUps = new SlidingWindow(20);
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
                            Package pkg = dbCtrler.getPackageByID(package_id);
                            pkg.setStatus("WAITPURCHASING");
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
        //long worldIDFromUps = recvWorldID();
        while(true) {
            try{
                // only for test
                Socket worldSocket = worldComm.connectToworldWithoudID(whs);
                //Socket worldSocket = worldComm.connectToWorld(worldIDFromUps, whs);
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
        try (ServerSocket serverSocket = new ServerSocket(UPS_SERVER_PORT);) {
            System.out.println("waiting for worldID");
            Socket clientSocket = serverSocket.accept(); 
            upsRecver = clientSocket.getInputStream();
            upsSender = clientSocket.getOutputStream();
            UAInitConnect.Builder msgB = UAInitConnect.newBuilder();
            
            Recver.recvMessage(msgB, upsRecver);
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
        Sender.sendMessage(msg.build(), out);
    }

    public static synchronized long getSeqnum() {
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
        if (hasRecved(arrived)) {
            return;
        }
        recvedSeqFromWorld.addSeqnum(arrived.getSeqnum());
        synchronized (unfinishedPackages) {
            for(Package p: unfinishedPackages){
                if(p.getStatus().equals("WAITPURCHASING") && p.getWh().getId() == arrived.getWhnum() && ProductToAProduct.hasSameProducts(arrived.getThingsList(), p.getProducts())){
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
        if (hasRecved(ready)) {
            return;
        }
        recvedSeqFromWorld.addSeqnum(ready.getSeqnum());
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
        if (hasRecved(loaded)) {
            return;
        }
        recvedSeqFromWorld.addSeqnum(loaded.getSeqnum());
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
        if (hasRecved(err)) {
            return;
        }
        recvedSeqFromWorld.addSeqnum(err.getSeqnum());
        System.out.println("Error: " + err.toString());
    }

    private void processPackageStatus(APackage packageStatus) {
        if (hasRecved(packageStatus)) {
            return;
        }
        recvedSeqFromWorld.addSeqnum(packageStatus.getSeqnum());
        System.out.println("Package status: " + packageStatus.toString());
    }

    // Methods processing the responses from the world over

    // Below are the methods to send commands to the world

    public void sendOneCmdsToWorld(ACommands cmds, Long seqnum, OutputStream out) throws UnknownHostException, IOException {
        // send commands to the world
        // use Timer, if no acks received in 10 seconds, resend the commands
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (out) {
                    System.out.println("sending to ups: " + cmds);
                    Sender.sendMessage(cmds, out);
                }
            }
        }, 0, 10000);
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
            e.printStackTrace();
        } catch (IOException e) {
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
        for(UAChangeAddr cmd : cmds.getChangeAddrList()) {
            processChangeAddr(cmd);
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
        if(hasRecved(arrived)) {
            return;
        }
        recvedSeqFromUps.addSeqnum(arrived.getSeqnum());
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
        if(hasRecved(delivered)) {
            return;
        }
        recvedSeqFromUps.addSeqnum(delivered.getSeqnum());
        synchronized(unfinishedPackages) {
            for(Package p : unfinishedPackages) {
                if(p.getTrackingID().equals(delivered.getTrackingid())){
                    p.setStatus("DELIVERED");
                    dbCtrler.updatePackageStatus(p.getPackageID(), "DELIVERED");
                    try {
                        if (p.getEmail() != null && !p.getEmail().isEmpty()) {
                            String msg = "Dear user " + p.getAmazonAccount() + 
                                        ", your package " + p.getTrackingID() + " has been delivered" +
                                        " to " + p.getDest().getXLocation() + ", " + p.getDest().getYLocation() +
                                        ". Thank you for using Amazon!";
                            emailSender.sendNotification(p.getEmail(), msg);
                        }
                    } catch (GeneralSecurityException | IOException e) {
                        e.printStackTrace();
                    }
                    unfinishedPackages.remove(p);
                    break;
                }
            }
        }
    }

    private void processChangeAddr(UAChangeAddr changeAddr) {
        if(hasRecved(changeAddr)) {
            return;
        }
        recvedSeqFromUps.addSeqnum(changeAddr.getSeqnum());
        synchronized(unfinishedPackages) {
            for(Package p : unfinishedPackages) {
                if(p.getTrackingID().equals(changeAddr.getTrackingid())){
                    p.setDest(new Location(changeAddr.getDestX(), changeAddr.getDestY()));
                    dbCtrler.updateDest(p.getPackageID(), changeAddr.getDestX(), changeAddr.getDestY());
                    break;
                }
            }
        }
    }

    private void processErrors(Err err) {
        if(hasRecved(err)) {
            return;
        }
        recvedSeqFromUps.addSeqnum(err.getSeqnum());
        System.out.println("UPS error: " + err.getMsg());
    }

    // Methods processing the commands from UPS over

    // Below are the methods to send commands to UPS

    public void sendOneCmdsToUps(AUCommands cmds, Long seqnum) {
        // send commands to UPS
        // use Timer, if no acks received in 10 seconds, resend the commands
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Sender.sendMessage(cmds, upsSender);
            }
        }, 0, 10000);
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
        Pack.Builder pack = Pack.newBuilder().setWhId(wh_id).addAllThings(products).setTrackingid(tracking_id).setPackageid(package_id).setAmazonaccount(amazonAccount).setDestX(dest_x).setDestY(dest_y);
        // what about ups account?
        if(pkg.getUpsAccount() != -1){
            pack.setUpsaccount(pkg.getUpsAccount());
        }
        AUNeedATruck needATruck = AUNeedATruck.newBuilder().setPack(pack.build()).setSeqnum(seqnum).build();
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

    // Helper methods for UPS sequence number
    private boolean hasRecved(UATruckArrived arrived) {
        return recvedSeqFromUps.containsSeqnum(arrived.getSeqnum());
    }

    private boolean hasRecved(UADelivered delivered) {
        return recvedSeqFromUps.containsSeqnum(delivered.getSeqnum());
    }

    private boolean hasRecved(UAChangeAddr changeAddr) {
        return recvedSeqFromUps.containsSeqnum(changeAddr.getSeqnum());
    }

    private boolean hasRecved(Err err) {
        return recvedSeqFromUps.containsSeqnum(err.getSeqnum());
    }

    // Helper methods for UPS sequence number over

    // Helper methods for World sequence number

    private boolean hasRecved(APurchaseMore arrived) {
        return recvedSeqFromWorld.containsSeqnum(arrived.getSeqnum());
    }

    private boolean hasRecved(APacked ready) {
        return recvedSeqFromWorld.containsSeqnum(ready.getSeqnum());
    }

    private boolean hasRecved(ALoaded loaded) {
        return recvedSeqFromWorld.containsSeqnum(loaded.getSeqnum());
    }

    private boolean hasRecved(AErr err) {
        return recvedSeqFromWorld.containsSeqnum(err.getSeqnum());
    }

    private boolean hasRecved(APackage packageStatus) {
        return recvedSeqFromWorld.containsSeqnum(packageStatus.getSeqnum());
    }

    // Helper methods for World sequence number over
}
