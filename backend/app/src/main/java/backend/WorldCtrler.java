package backend;

import java.io.*;
import java.net.*;
import java.util.*;

import backend.protocol.WorldAmazon.*;
import backend.utils.Recver;
import backend.utils.Sender;

public class WorldCtrler {

    public static Socket connectToworldWithoudID(List<WareHouse> whs) throws UnknownHostException, IOException {
        AConnect msgToSend = new WorldMsger().connectWithoutID(whs);
        // set up the TCP connection to the world
        Socket socket = new Socket("vcm-37900.vm.duke.edu", 23456);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        // connect to the world(send AConnect message)
        Sender.sendMsgTo(msgToSend, out);
        // receive the response from the world
        AConnected.Builder connected = AConnected.newBuilder();
        Recver.recvMsgFrom(connected, in);
        System.out.println("world id: " + connected.getWorldid());
        System.out.println("result: " + connected.getResult());
        if(connected.getResult().equals("connected!")) {
            return socket;
        } else {
            System.out.println("Failed to connect to the world!");
            return null;
        }
    }

    public static Socket connectToWorld(long worldid, List<WareHouse> whs) throws UnknownHostException, IOException {
        AConnect msgToSend = new WorldMsger().connect(worldid, whs);
        // set up the TCP connection to the world
        Socket socket = new Socket("localhost", 23456);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        // connect to the world(send AConnect message)
        out.write(msgToSend.toByteArray());
        // receive the response from the world
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        AConnected connected = AConnected.parseFrom(Arrays.copyOf(buffer, len));
        System.out.println("world id: " + connected.getWorldid());
        System.out.println("result: " + connected.getResult());
        if(connected.getResult().equals("connected!")) {
            return socket;
        } else {
            System.out.println("Failed to connect to the world!");
            return null;
        }
    }

    public static void sendOneCmds(ACommands cmds, List<Long> seqnums, InputStream in, OutputStream out) throws UnknownHostException, IOException {
        // send commands to the world
        synchronized (out) {
            Sender.sendMsgTo(cmds, out);
        }
        // receive the response from the world
        AResponses.Builder responsesB = AResponses.newBuilder();
        synchronized (in) {
            Recver.recvMsgFrom(responsesB, in);
        }
        AResponses responses = responsesB.build();
        // check if the received acks match the seqnums
        if (checkAcks(responses, seqnums)) {
            System.out.println("Received acks match the seqnums.");
        } else {
            System.out.println("Received acks do not match the seqnums. Resending commands...");
            // resend the commands
            synchronized (out) {
                Sender.sendMsgTo(cmds, out);
            }
        }
    }

    private static boolean checkAcks(AResponses responses, List<Long> seqnums) {
        List<Long> receivedAcks = responses.getAcksList();
        return receivedAcks.equals(seqnums);
    }

    public static AResponses RecvOneRspsFromWorld(InputStream in, OutputStream out){
        // receive the response from the world
        try {
            AResponses.Builder responsesB = AResponses.newBuilder();
            System.out.println("Try to receive message from worldRecver");
            synchronized (in) {
                Recver.recvMsgFrom(responsesB, in);
            }
            AResponses responses = responsesB.build();
            System.out.println("Received from world: " + responses.toString());
            synchronized (out) {
                sendAcksToWorld(responses, out);
            }
            return responses;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void sendAcksToWorld(AResponses responses, OutputStream out) throws IOException {
        // get ack numbers
        List<Long> acks = new ArrayList<>();
        for (APurchaseMore a : responses.getArrivedList()) {
            acks.add(a.getSeqnum());
        }
        for (APacked a : responses.getReadyList()) {
            acks.add(a.getSeqnum());
        }
        for (ALoaded a : responses.getLoadedList()) {
            acks.add(a.getSeqnum());
        }
        for (AErr a : responses.getErrorList()) {
            acks.add(a.getSeqnum());
        }
        for (APackage a : responses.getPackagestatusList()) {
            acks.add(a.getSeqnum());
        }
        if (acks.size() > 0) {
            ACommands.Builder commands = ACommands.newBuilder();
            for (long seq : acks) {
                commands.addAcks(seq);
            }
            System.out.println("send ack back(to World): " + commands.toString());
            Sender.sendMsgTo(commands.build(), out);
        }
    }

    public void processWorldMsgs(AResponses reps) {
        // send acks back to the world
        // try {
        //     sendAcksToWorld(reps, out);
        // } catch (IOException e) {
        //     processWorldMsgs(reps, out); // better way?
        // }
        // process the responses
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
        List<Package> packages = Amazon.getPackages();
        synchronized (packages) {
            List<Long> arrivedProductIDs = new ArrayList<>();
            for (AProduct p : arrived.getThingsList()) {
                arrivedProductIDs.add(p.getId());
            }
            for(Package p: packages){
                if(p.getWh().getId() == arrived.getWhnum() && p.getProductIDs().equals(arrivedProductIDs)){
                    System.out.println("Arrived: " + arrived.toString());
                    packages.remove(p);
                    break;
                }
            }
            // tell UPS to send a truck
            // TODO: tell UPS to send a truck
            // tell world to pack
        }
    }

    private void processReady(APacked ready) {
        // if the truck has arrived, send load to world
        
        // 1. get the package
        List<Package> packages = Amazon.getPackages();
        synchronized (packages) {
            Package target = null;
            for(Package p: packages){
                if(p.getPackageID().equals(ready.getShipid())){
                    target = p;
                    break;
                }
            }
            // 2. TODO: send load to world
        }
    }

    private void processLoaded(ALoaded loaded) {
        // 1. get the package
        List<Package> packages = Amazon.getPackages();
        synchronized (packages) {
            Package target = null;
            for(Package p: packages){
                if(p.getPackageID().equals(loaded.getShipid())){
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

}
