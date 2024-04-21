package backend;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import backend.protocol.AmazonUps.Err;
import backend.protocol.AmazonUps.UACommands;
import backend.protocol.AmazonUps.UADelivered;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.protocol.WorldAmazon.*;

public class Amazon {
    private static final int FRONTEND_SERVER_PORT = 8888; 
    private static final int UPS_SERVER_PORT = 9999; 
    private static final int THREAD_POOL_SIZE = 10;

    private ExecutorService threadPool;
    private WorldCtrler worldCtrler;
    private ServerForUps upsServer;

    private static long seqnum;
    private final List<WareHouse> whs;
    private List<Package> unfinishedPackages;
    // fields for communication with world
    private InputStream worldRecver;
    private OutputStream worldSender;

    public Amazon() {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        worldCtrler = new WorldCtrler();
        upsServer = new ServerForUps();
        seqnum = 0;
        whs = new ArrayList<>();
        unfinishedPackages = new ArrayList<>();
    }

    public void startWorldRecver() {
        // start a thread to receive message from world
        Thread worldRecverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    AResponses res = worldCtrler.RecvOneRspsFromWorld(worldRecver, worldSender);
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
                            UACommands cmds = upsServer.recvOneCmdsFromUps(clientSocket);
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
                            ServerForFrontend.processFrontendMsgs(clientSocket);
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
        //long worldIDFromUps = upsServer.recvWorldID();
        while(true) {
            try{
                // only for test
                System.out.println("try to connect to world");
                Socket worldSocket = worldCtrler.connectToworldWithoudID(whs);
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
        for (APackage packageStatus : reps.getPackagestatusList()) {
            processPackageStatus(packageStatus);
        }
    }

    private void processArrived(APurchaseMore arrived) {
        synchronized (unfinishedPackages) {
            for(Package p: unfinishedPackages){
                if(p.getWh().getId() == arrived.getWhnum() && p.getProducts() == arrived.getThingsList()){
                    System.out.println("Arrived: " + arrived.toString());
                    sendToPack(p);
                    // tell UPS to send a truck
                    // TODO: tell UPS to send a truck
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
                    sendToLoad(p);
                    break;
                }
            }
        }
    }

    private void processLoaded(ALoaded loaded) {
        // 1. get the package
        synchronized (unfinishedPackages) {
            Package target = null;
            for(Package p: unfinishedPackages){
                if(p.getPackageID() == loaded.getShipid()){
                    target = p;
                    break;
                }
            }
            // 2. TODO: send toDeliver to UPS
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

    public void sendToPack(Package pkg){
        int whnum = pkg.getWh().getId();
        List<AProduct> things = pkg.getProducts();
        long shipid = pkg.getPackageID();
        long seqnum = Amazon.getSeqnum();
        WorldMsger msger = new WorldMsger();
        msger.pack(whnum, things, shipid, seqnum);
        List<Long> seqnums = new ArrayList<>();
        seqnums.add(seqnum);
        try {
            worldCtrler.sendOneCmds(msger.getCommands(), seqnums, worldRecver, worldSender);
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
        List<Long> seqnums = new ArrayList<>();
        seqnums.add(seqnum);
        try {
            worldCtrler.sendOneCmds(msger.getCommands(), seqnums, worldRecver, worldSender);
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
    }

    private void processArrived(UATruckArrived arrived) {
        int truckID = arrived.getTruckid();
        synchronized(unfinishedPackages) {
            for(Package p : unfinishedPackages) {
                if(p.getPackageID() == arrived.getPackageid()){
                    p.setTruckID(truckID);
                    // if this package is packed, tell world to load
                    if(p.getStatus() == "PACKED"){
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


}
