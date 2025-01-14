package backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import org.junit.jupiter.api.Test;
import backend.protocol.WorldAmazon.*;
import backend.utils.Triplet;

public class WorldMsgerTest {
    WorldMsger worldConnector = new WorldMsger();

    @Test
    public void testConnect() {
        long worldId = 123456L;
        List<WareHouse> warehouses = new ArrayList<>();
        warehouses.add(new WareHouse(1, new Location(1, 2)));
        warehouses.add(new WareHouse(2, new Location(3, 4)));

        AConnect aConnect = worldConnector.connect(worldId, warehouses);

        assertEquals(worldId, aConnect.getWorldid());
        assertEquals(true, aConnect.getIsAmazon());
        assertEquals(warehouses.size(), aConnect.getInitwhCount());
        for (int i = 0; i < warehouses.size(); i++) {
            AInitWarehouse initWarehouse = aConnect.getInitwh(i);
            Location location = warehouses.get(i).getLocation();
            assertEquals(location.getXLocation(), initWarehouse.getX());
            assertEquals(location.getYLocation(), initWarehouse.getY());
        }
    }

    @Test
    public void testPurchaseMore() {
        int warehouseNumber = 1;
        List<AProduct> products = new ArrayList<>();
        products.add(AProduct.newBuilder().setId(1).setDescription("Product1").setCount(10).build());
        products.add(AProduct.newBuilder().setId(2).setDescription("Product2").setCount(20).build());
        long sequenceNumber = 123;
        worldConnector.purchaseMore(warehouseNumber, products, sequenceNumber);
        ACommands commands = worldConnector.getCommands();
        APurchaseMore purchaseMore = commands.getBuy(0);
        assertEquals(warehouseNumber, purchaseMore.getWhnum());
        assertEquals(2, purchaseMore.getThingsCount()); 
        assertEquals(sequenceNumber, purchaseMore.getSeqnum());
    }

    @Test
    public void testPack() {
        int warehouseNumber = 1;
        List<AProduct> products = new ArrayList<>();
        products.add(AProduct.newBuilder().setId(1).setDescription("Product1").setCount(10).build());
        products.add(AProduct.newBuilder().setId(2).setDescription("Product2").setCount(20).build());
        long shipId = 456;
        long sequenceNumber = 123;

        worldConnector.pack(warehouseNumber, products, shipId, sequenceNumber);
        ACommands commands = worldConnector.getCommands();
        APack packCommand = commands.getTopack(0);
        assertEquals(warehouseNumber, packCommand.getWhnum());
        assertEquals(2, packCommand.getThingsCount()); 
        assertEquals(shipId, packCommand.getShipid());
        assertEquals(sequenceNumber, packCommand.getSeqnum());
    }

    @Test
    public void testLoad() {
        int warehouseNumber = 1;
        int truckId = 123;
        long shipId = 456;
        long sequenceNumber = 789;

        worldConnector.load(warehouseNumber, truckId, shipId, sequenceNumber);
        ACommands commands = worldConnector.getCommands();
        APutOnTruck loadCommand = commands.getLoad(0);
        assertEquals(warehouseNumber, loadCommand.getWhnum());
        assertEquals(truckId, loadCommand.getTruckid());
        assertEquals(shipId, loadCommand.getShipid());
        assertEquals(sequenceNumber, loadCommand.getSeqnum());
    }

    @Test
    public void testQuery() {
        long packageId = 123;
        long sequenceNumber = 456;

        worldConnector.query(packageId, sequenceNumber);
        ACommands commands = worldConnector.getCommands();
        AQuery queryCommand = commands.getQueries(0);
        assertEquals(packageId, queryCommand.getPackageid());
        assertEquals(sequenceNumber, queryCommand.getSeqnum());
    }

    @Test
    public void testSetAcks() {
        long[] acks = {1, 2, 3, 4, 5};

        worldConnector.setAcks(acks);
        ACommands commands = worldConnector.getCommands();
        assertEquals(acks.length, commands.getAcksCount());
        for (int i = 0; i < acks.length; i++) {
            assertEquals(acks[i], commands.getAcks(i));
        }
    }

    @Test
    public void testSetSimSpeed() {
        int simSpeed = 10;

        worldConnector.setSimSpeed(simSpeed);
        ACommands commands = worldConnector.getCommands();
        assertEquals(simSpeed, commands.getSimspeed());
    }

    @Test
    public void testDisconnect() {
        worldConnector.disconnect();
        ACommands commands = worldConnector.getCommands();
        assertTrue(commands.getDisconnect());
    }

    @Test
    public void testAllFunctions() {
        int warehouseNumber = 1;
        List<AProduct> products = new ArrayList<>();
        products.add(AProduct.newBuilder().setId(1).setDescription("Product1").setCount(10).build());
        products.add(AProduct.newBuilder().setId(2).setDescription("Product2").setCount(20).build());
        long shipId = 456;
        long sequenceNumber = 789;
        long packageId = 123;
        int simSpeed = 10;
        int truckId = 456;
        long[] acks = {1, 2, 3};

        worldConnector.purchaseMore(warehouseNumber, products, sequenceNumber);
        worldConnector.pack(warehouseNumber, products, shipId, sequenceNumber);
        worldConnector.load(warehouseNumber, truckId, shipId, sequenceNumber);
        worldConnector.query(packageId, sequenceNumber);
        worldConnector.setAcks(acks);
        worldConnector.setSimSpeed(simSpeed);
        worldConnector.disconnect();

        ACommands commands = worldConnector.getCommands();

        assertEquals(warehouseNumber, commands.getBuy(0).getWhnum());
        assertEquals(warehouseNumber, commands.getTopack(0).getWhnum());
        assertEquals(warehouseNumber, commands.getLoad(0).getWhnum());
        assertEquals(packageId, commands.getQueries(0).getPackageid());
        assertEquals(simSpeed, commands.getSimspeed());
        assertTrue(commands.getDisconnect());
        assertEquals(acks.length, commands.getAcksCount());
        for (int i = 0; i < acks.length; i++) {
            assertEquals(acks[i], commands.getAcks(i));
        }
    }

    // test for response parser
    @Test
    public void testCheckArrival() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        int warehouseNumber1 = 1;
        List<AProduct> products1 = new ArrayList<>();
        products1.add(AProduct.newBuilder().setId(1).setDescription("Product1").setCount(10).build());
        products1.add(AProduct.newBuilder().setId(2).setDescription("Product2").setCount(20).build());
        long seqnum1 = 123;
        APurchaseMore arrived1 = APurchaseMore.newBuilder().setWhnum(warehouseNumber1).addAllThings(products1).setSeqnum(seqnum1).build();
        responsesBuilder.addArrived(arrived1);
        int warehouseNumber2 = 2;
        List<AProduct> products2 = new ArrayList<>();
        products2.add(AProduct.newBuilder().setId(3).setDescription("Product3").setCount(30).build());
        products2.add(AProduct.newBuilder().setId(4).setDescription("Product4").setCount(40).build());
        long seqnum2 = 456;
        APurchaseMore arrived2 = APurchaseMore.newBuilder().setWhnum(warehouseNumber2).addAllThings(products2).setSeqnum(seqnum2).build();
        responsesBuilder.addArrived(arrived2);

        AResponses responses = responsesBuilder.build();

        List<Triplet<Integer, List<AProduct>, Long>> returnedArrivedProducts = worldConnector.getArrived(responses);
        assertEquals(2, returnedArrivedProducts.size());
        assertEquals(warehouseNumber1, returnedArrivedProducts.get(0).getFirst());
        assertEquals(warehouseNumber2, returnedArrivedProducts.get(1).getFirst());
        assertEquals(products1.size(), returnedArrivedProducts.get(0).getSecond().size());
        assertEquals(products2.size(), returnedArrivedProducts.get(1).getSecond().size());
        for (int i = 0; i < products1.size(); i++) {
            assertEquals(products1.get(i), returnedArrivedProducts.get(0).getSecond().get(i));
        }
        for (int i = 0; i < products2.size(); i++) {
            assertEquals(products2.get(i), returnedArrivedProducts.get(1).getSecond().get(i));
        }
        assertEquals(seqnum1, returnedArrivedProducts.get(0).getThird());
        assertEquals(seqnum2, returnedArrivedProducts.get(1).getThird());
    }

    @Test
    public void testCheckPacked() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        List<Long> packedShipIds = List.of(123L, 456L, 789L);
        long seqnum = 123;
        for (long shipId : packedShipIds) {
            APacked packed = APacked.newBuilder().setShipid(shipId).setSeqnum(seqnum).build();
            responsesBuilder.addReady(packed);
        }

        AResponses responses = responsesBuilder.build();

        Map<Long, Long> returnedPackedShipIds = worldConnector.getPacked(responses);
        assertEquals(packedShipIds.size(), returnedPackedShipIds.size());
        for (long shipId : packedShipIds) {
            assertEquals(seqnum, returnedPackedShipIds.get(shipId));
        }
    }

    @Test
    public void testCheckLoaded() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        long seqnum = 123;
        List<Long> loadedShipIds = List.of(123L, 456L, 789L);
        for (long shipId : loadedShipIds) {
            ALoaded loaded = ALoaded.newBuilder().setShipid(shipId).setSeqnum(seqnum).build();
            responsesBuilder.addLoaded(loaded);
        }

        AResponses responses = responsesBuilder.build();

        Map<Long, Long> returnedLoadedShipIds = worldConnector.getLoaded(responses);
        assertEquals(loadedShipIds.size(), returnedLoadedShipIds.size());
        for (long shipId : loadedShipIds) {
            assertEquals(seqnum, returnedLoadedShipIds.get(shipId));
        }
    }

    @Test
    public void testCheckFinished() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        responsesBuilder.setFinished(true);
        AResponses responses = responsesBuilder.build();

        boolean finished = worldConnector.checkFinished(responses);
        assertTrue(finished);
    }

    @Test
    public void testCheckErrs() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        List<AErr> errorList = new ArrayList<>();
        errorList.add(AErr.newBuilder().setErr("Error1").setOriginseqnum(123L).setSeqnum(456L).build());
        errorList.add(AErr.newBuilder().setErr("Error2").setOriginseqnum(789L).setSeqnum(101112L).build());
        for (AErr err : errorList) {
            responsesBuilder.addError(err);
        }

        AResponses responses = responsesBuilder.build();

        List<Triplet<String, Long, Long>> errs = worldConnector.getErrs(responses);
        assertEquals(errorList.size(), errs.size());
        for (int i = 0; i < errorList.size(); i++) {
            assertEquals(errorList.get(i).getErr(), errs.get(i).getFirst());
            assertEquals(errorList.get(i).getOriginseqnum(), errs.get(i).getSecond());
            assertEquals(errorList.get(i).getSeqnum(), errs.get(i).getThird());
        }
    }

    @Test
    public void testGetAcks() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        List<Long> acksList = new ArrayList<>();
        acksList.add(123L);
        acksList.add(456L);
        acksList.add(789L);
        for (Long ack : acksList) {
            responsesBuilder.addAcks(ack);
        }

        AResponses responses = responsesBuilder.build();

        List<Long> retrievedAcks = worldConnector.getAcks(responses);
        assertEquals(acksList.size(), retrievedAcks.size());
        for (int i = 0; i < acksList.size(); i++) {
            assertEquals(acksList.get(i), retrievedAcks.get(i));
        }
    }

    @Test
    public void testGetStatus() {
        AResponses.Builder responsesBuilder = AResponses.newBuilder();

        List<APackage> packageStatusList = new ArrayList<>();
        packageStatusList.add(APackage.newBuilder().setPackageid(123L).setStatus("Delivered").setSeqnum(456L).build());
        packageStatusList.add(APackage.newBuilder().setPackageid(456L).setStatus("In transit").setSeqnum(789L).build());
        packageStatusList.add(APackage.newBuilder().setPackageid(789L).setStatus("Processing").setSeqnum(101112L).build());
        for (APackage stat : packageStatusList) {
            responsesBuilder.addPackagestatus(stat);
        }
        AResponses responses = responsesBuilder.build();

        List<Triplet<Long, String, Long>> retrievedStatus = worldConnector.getStatus(responses);
        assertEquals(packageStatusList.size(), retrievedStatus.size());
        for (int i = 0; i < packageStatusList.size(); i++) {
            assertEquals(packageStatusList.get(i).getPackageid(), retrievedStatus.get(i).getFirst());
            assertEquals(packageStatusList.get(i).getStatus(), retrievedStatus.get(i).getSecond());
            assertEquals(packageStatusList.get(i).getSeqnum(), retrievedStatus.get(i).getThird());
        }
    }
}
