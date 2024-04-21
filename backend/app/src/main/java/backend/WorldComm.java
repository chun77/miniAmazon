package backend;

import java.io.*;
import java.net.*;
import java.util.*;

import backend.protocol.WorldAmazon.*;
import backend.utils.Recver;
import backend.utils.Sender;

public class WorldComm {

    public Socket connectToworldWithoudID(List<WareHouse> whs) throws UnknownHostException, IOException {
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

    public Socket connectToWorld(long worldid, List<WareHouse> whs) throws UnknownHostException, IOException {
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

    public void sendOneCmds(ACommands cmds, List<Long> seqnums, InputStream in, OutputStream out) throws UnknownHostException, IOException {
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

    private boolean checkAcks(AResponses responses, List<Long> seqnums) {
        List<Long> receivedAcks = responses.getAcksList();
        return receivedAcks.equals(seqnums);
    }

    public AResponses RecvOneRspsFromWorld(InputStream in, OutputStream out){
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

    private void sendAcksToWorld(AResponses responses, OutputStream out) throws IOException {
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

    

}
