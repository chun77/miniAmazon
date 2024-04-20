package backend;

import java.util.*;

import com.google.protobuf.ByteString.Output;

import backend.protocol.WorldAmazon.AResponses;
import backend.utils.Recver;

import java.io.*;
import java.net.*;

public class Amazon {
    private static final int FRONTEND_SERVER_PORT = 8888; 
    private static final int UPS_SERVER_PORT = 9999; 
    private ServerForFrontend frontendServer;
    private ServerForUps upsServer;
    private WorldCtrler worldCtrler;
    private ClientForUps upsClient;

    private long seqnum;
    private final List<WareHouse> whs;
    // fields for communication with world
    private InputStream worldRecver;
    private OutputStream worldSender;

    public Amazon() {
        seqnum = 0;
        frontendServer = new ServerForFrontend();
        upsServer = new ServerForUps();
        worldCtrler = new WorldCtrler();
        upsClient = new ClientForUps();
        whs = new ArrayList<>();
    }

    public void startWorldRecver() {
        // start a thread to receive message from world
        Thread worldRecverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    AResponses res = WorldCtrler.RecvOneRspsFromWorld(worldRecver, worldSender);
                    System.out.println("Received from world: " + res);
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
                        // TODO: receive message from ups
                        System.out.println("Client connected: " + clientSocket);
                        //ClientHandler clientHandler = new ClientHandler(clientSocket);
                        //Thread clientThread = new Thread(clientHandler);
                        //clientThread.start();
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
                        // TODO: receive message from frontend
                        System.out.println("Client connected: " + clientSocket);
                        // ClientHandler clientHandler = new ClientHandler(clientSocket);
                        // Thread clientThread = new Thread(clientHandler);
                        // clientThread.start();
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

    

    public long getSeqnum() {
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

    // 1. setup server, waiting for frontend
    // 2. receive order from frontend
    // 3. according to the products, determine which warehouse should be used  
    // 4. send topack to world, and send need a truck to UPS
    // 5. waiting until receive packed and truck arrived
    // 6. send load to world
    // 7. waiting until receive loaded
    // 8. send truck can go to UPS
    // 9. waiting until receive delivered, then change the status of order to delivered
}
