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
}
