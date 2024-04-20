package backend;

import java.io.*;
import java.net.*;

public class ServerForUps {
    // need to improve
    // public long recvWorldID() {
    //     try (ServerSocket serverSocket = new ServerSocket(PORT);) {
    //         Socket clientSocket = serverSocket.accept(); 
    //         if(clientSocket == null) {
    //             return recvWorldID();
    //         }
    //         UAConnect msg = UAConnect.parseFrom(clientSocket.getInputStream());
    //         long worldID = msg.getWorldid();
    //         return worldID;
    //     } catch (IOException e) {
    //         return recvWorldID();
    //     }
    // }

    // TODO: ups server message handler

    public static void processUpsMsgs(Socket clientSocket) {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            System.out.println("Received: " + new String(buffer, 0, len));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
