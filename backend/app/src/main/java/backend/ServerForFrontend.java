package backend;

import java.io.*;
import java.net.*;

public class ServerForFrontend {

    public static void processFrontendMsgs(Socket clientSocket) {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            System.out.println("Received: " + new String(buffer, 0, len));
            out.write("Order received".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
}
