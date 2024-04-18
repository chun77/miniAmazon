package backend;

import java.io.*;
import java.net.*;
import java.util.*;

import backend.protocol.WorldAmazon.*;

public class ClientForWorld {
    public boolean connectToWorld(long worldid, Map<Integer, Location> whs) throws UnknownHostException, IOException {
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
        socket.close();
        return connected.getResult().equals("connected!");
    }

    public void sendCmds(ACommands cmds, List<Long> seqnums) throws UnknownHostException, IOException {
        // set up the TCP connection to the world
        Socket socket = new Socket("localhost", 23456);
        socket.setSoTimeout(3000); // set timeout to 3 seconds
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        // send commands to the world
        out.write(cmds.toByteArray());
        // receive the response from the world
        byte[] buffer = new byte[1024];
        try {
            int len = in.read(buffer);
            AResponses responses = AResponses.parseFrom(Arrays.copyOf(buffer, len));
            // check if the received acks match the seqnums
            if (checkAcks(responses, seqnums)) {
                System.out.println("Received acks match the seqnums.");
            } else {
                System.out.println("Received acks do not match the seqnums. Resending commands...");
                // resend the commands
                out.write(cmds.toByteArray());
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout: No response received within 3 seconds. Resending commands...");
            // resend the commands
            out.write(cmds.toByteArray());
        }
        
        socket.close();
    }

    private boolean checkAcks(AResponses responses, List<Long> seqnums) {
        List<Long> receivedAcks = responses.getAcksList();
        return receivedAcks.equals(seqnums);
    }

}
