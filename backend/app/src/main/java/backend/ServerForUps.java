package backend;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString.Output;

import backend.protocol.AmazonUps.AUCommands;
import backend.protocol.AmazonUps.Err;
import backend.protocol.AmazonUps.UACommands;
import backend.protocol.AmazonUps.UADelivered;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.utils.Recver;
import backend.utils.Sender;

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

    public static UACommands recvOneCmdsFromUps(Socket clientSocket){
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            UACommands.Builder cmdsB = UACommands.newBuilder();
            Recver.recvMsgFrom(cmdsB, in);
            UACommands cmds = cmdsB.build();
            sendAcksToUps(cmds, out);
            return cmds;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void sendAcksToUps(UACommands cmds, OutputStream out) throws IOException {
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
        
    }

    private void processDelivered(UADelivered delivered) {
    }

    private void processErrors(Err err) {
    }
    
}
