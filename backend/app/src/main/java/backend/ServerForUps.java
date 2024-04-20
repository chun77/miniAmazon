package backend;

import java.io.*;
import java.net.*;

public class ServerForUps {
    private static final int PORT = 9999; 

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

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    System.out.println("Received from client: " + inputLine);

                    String response = "Server received: " + inputLine;
                    writer.println(response);
                }
                reader.close();
                writer.close();
                clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
