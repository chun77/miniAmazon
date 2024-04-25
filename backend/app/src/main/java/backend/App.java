/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package backend;

import java.io.*;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.*;

import backend.protocol.WorldAmazon.AProduct;
import backend.protocol.WorldAmazon.ACommands;
import backend.utils.DBCtrler;
import backend.utils.EmailSender;
import backend.utils.Sender;

public class App {

    public static void main(String[] args) {
        // test email sending
        // EmailSender emailSender = new EmailSender();
        // try {
        //     emailSender.sendNotification("steven.h.geng@gmail.com", "hihihi");
        // } catch (GeneralSecurityException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // } catch (IOException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }

        Amazon amazon = new Amazon();
        amazon.initialize();
        OutputStream worldSender = amazon.getWorldSender();

        //set database (only for testing)
        //DBCtrler.dropTables();
        //DBCtrler.createTables();
        //DBCtrler.initializeTables();

        // set simspeed
        WorldMsger worldMsger = new WorldMsger();
        worldMsger.setSimSpeed(500); // only for testing
        System.out.println("set simspeed to 500");
        Sender.sendMsgTo(worldMsger.getCommands(), worldSender);
        System.out.println("set simspeed to 500 success");

        // start 3 threads, for world receiver, ups server and frontend server respectively
        amazon.startWorldRecver();
        //amazon.startUpsServer();
        amazon.startFrontendServer();

        
        // send two purchaseMore messages to the world, just for test
        // List<AProduct> products = new ArrayList<>();
        // products.add(AProduct.newBuilder().setId(6).setDescription("testProduct").setCount(10).build());
        // worldMsger = new WorldMsger();
        // worldMsger.purchaseMore(1, products, 1);
        // ACommands cmds = worldMsger.getCommands();
        // try {
        //     amazon.sendOneCmdsToWorld(cmds, 1L, worldSender);
        // } catch (UnknownHostException e) {
        //     e.printStackTrace();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        // try {
        //     Thread.sleep(10000);
        // } catch (InterruptedException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
        // worldMsger = new WorldMsger();
        // worldMsger.purchaseMore(1, products, 2);
        // cmds = worldMsger.getCommands();
        // try {
        //     amazon.sendOneCmdsToWorld(cmds, 2L, worldSender);
        // } catch (UnknownHostException e) {
        //     e.printStackTrace();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        
    }
}
