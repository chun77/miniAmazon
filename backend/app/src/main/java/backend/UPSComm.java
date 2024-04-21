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
import backend.protocol.AmazonUps.UAInitConnect;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.utils.Recver;
import backend.utils.Sender;

public class UPSComm {
    public long recvWorldID() {
        try (ServerSocket serverSocket = new ServerSocket(9999);) {
            Socket clientSocket = serverSocket.accept(); 
            InputStream in = clientSocket.getInputStream();
            UAInitConnect.Builder msgB = UAInitConnect.newBuilder();
            Recver.recvMsgFrom(msgB, in);
            long worldID = msgB.getWorldid();
            return worldID;
        } catch (IOException e) {
            return recvWorldID();
        }
    }

    public UACommands recvOneCmdsFromUps(Socket clientSocket){
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

    public void sendOneCmds(AUCommands cmds, List<Long> seqnums) {
        try {
            Socket socket = new Socket("ups_ip", 9998);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            Sender.sendMsgTo(cmds, out);
            // wait for acks
            UACommands.Builder acksB = UACommands.newBuilder();
            Recver.recvMsgFrom(acksB, in);
            if(checkAcks(acksB.build(), seqnums)) {
                System.out.println("Received acks from UPS");
                socket.close();
            } else {
                sendOneCmds(cmds, seqnums);
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendOneCmds(cmds, seqnums);
        }
    }

    private boolean checkAcks(UACommands acks, List<Long> seqnums) {
        List<Long> receivedAcks = acks.getAcksList();
        return receivedAcks.equals(seqnums);
    }
    
}
