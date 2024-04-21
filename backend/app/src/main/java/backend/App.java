/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package backend;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

import backend.protocol.WorldAmazon.AProduct;
import backend.protocol.WorldAmazon.ACommands;
import backend.utils.DBCtrler;
import backend.utils.Sender;

public class App {

    public static void main(String[] args) {
        Amazon amazon = new Amazon();
        WorldComm worldCtrler = new WorldComm();
        amazon.initialize();
        InputStream worldRecver = amazon.getWorldRecver();
        OutputStream worldSender = amazon.getWorldSender();

        // set database (only for testing)
        // DBCtrler.dropTables();
        // DBCtrler.createTables();
        // DBCtrler.initializeTables();

        // set simspeed
        WorldMsger worldMsger = new WorldMsger();
        worldMsger.setSimSpeed(500); // only for testing
        System.out.println("set simspeed to 500");
        Sender.sendMsgTo(worldMsger.getCommands(), worldSender);
        System.out.println("set simspeed to 500 success");

        // start 3 threads, for world receiver, ups server and frontend server respectively
        amazon.startWorldRecver();
        amazon.startUpsServer();
        amazon.startFrontendServer();

        // send a topack message to the world
        List<AProduct> products = new ArrayList<>();
        products.add(AProduct.newBuilder().setId(1).setDescription("Product1").setCount(10).build());
        worldMsger = new WorldMsger();
        worldMsger.purchaseMore(1, products, 1);
        worldMsger.setSimSpeed(10000); // only for testing
        ACommands cmds = worldMsger.getCommands();
        try {
            amazon.sendOneCmdsToWorld(cmds, 1L, worldSender);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
