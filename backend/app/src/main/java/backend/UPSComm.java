package backend;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import backend.protocol.AmazonUps.AUCommands;
import backend.protocol.AmazonUps.AUConfirmConnect;
import backend.protocol.AmazonUps.UACommands;
import backend.protocol.AmazonUps.UADelivered;
import backend.protocol.AmazonUps.UAInitConnect;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.utils.Recver;
import backend.utils.Sender;

public class UPSComm {
    public long recvWorldID() {
        try (ServerSocket serverSocket = new ServerSocket(9999);) {
            System.out.println("waiting for worldID");
            Socket clientSocket = serverSocket.accept(); 
            InputStream in = clientSocket.getInputStream();
            UAInitConnect.Builder msgB = UAInitConnect.newBuilder();
            
            Recver.recvMsgFrom(msgB, in);
            long worldID = msgB.getWorldid();
            sendBackConnected(worldID, clientSocket.getOutputStream());
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
        Sender.sendMsgTo(msg.build(), out);
    }

    // public UACommands recvOneCmdsFromUps(Socket clientSocket){
    //     try {
    //         InputStream in = clientSocket.getInputStream();
    //         OutputStream out = clientSocket.getOutputStream();
    //         UACommands.Builder cmdsB = UACommands.newBuilder();
    //         Recver.recvMsgFrom(cmdsB, in);
    //         UACommands cmds = cmdsB.build();
    //         sendAcksToUps(cmds, out);
    //         return cmds;
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //         return null;
    //     }
    // }

    public UACommands recvOneCmdsFromUps(InputStream in, OutputStream out){
        try {
            UACommands.Builder cmdsB = UACommands.newBuilder();
            Recver.recvMsgFrom(cmdsB, in);
            System.out.println("received cmds from UPS: " + cmdsB.toString());
            UACommands cmds = cmdsB.build();
            sendAcksToUps(cmds, out);
            return cmds;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendAcksToUps(UACommands cmds, OutputStream out) throws IOException {
        // get ack numbers
        List<Long> acks = new ArrayList<>();
        for(UATruckArrived cmd : cmds.getArrivedList()) {
            acks.add(cmd.getSeqnum());
        }
        for(UADelivered cmd : cmds.getDeliveredList()) {
            acks.add(cmd.getSeqnum());
        }
        if (acks.size() > 0) {
            AUCommands.Builder commands = AUCommands.newBuilder();
            for (long seq : acks) {
                commands.addAcks(seq);
            }
            System.out.println("send ack back(to UPS): " + commands.toString());
            Sender.sendMsgTo(commands.build(), out);
        }
    }

    
}
