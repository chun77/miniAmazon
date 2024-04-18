package backend;

import java.util.*;

public class Amazon {
    
    ServerForFrontend frontendServer;
    ServerForUps upsServer;
    ClientForWorld worldClient;
    ClientForUps upsClient;

    private long seqnum;
    private final List<WareHouse> whs;

    public Amazon() {
        seqnum = 0;
        frontendServer = new ServerForFrontend();
        upsServer = new ServerForUps();
        worldClient = new ClientForWorld();
        upsClient = new ClientForUps();
        whs = new ArrayList<>();
    }

    public long getSeqnum() {
        long tmp = seqnum;
        seqnum++;
        return tmp;
    }

    public void initialize() {
        // TODO: initialize the warehouse
        long worldIDFromUps = upsServer.recvWorldID();
        while(true) {
            try{
                if(worldClient.connectToWorld(worldIDFromUps, whs)) {
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    // 1. setup server, waiting for frontend
    // 2. receive order from frontend
    // 3. according to the products, determine which warehouse should be used  
    // 4. send topack to world, and send need a truck to UPS
    // 5. waiting until receive packed and truck arrived
    // 6. send load to world
    // 7. waiting until receive loaded
    // 8. send truck can go to UPS
    // 9. waiting until receive delivered, then change the status of order to delivered
}
