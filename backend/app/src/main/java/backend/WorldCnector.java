package backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.protocol.WorldAmazon.*;
import backend.utils.Triplet;

public class WorldCnector {
    private ACommands.Builder cmdsBuilder;

    public WorldCnector() {
        cmdsBuilder = ACommands.newBuilder();
    }

    public ACommands getCommands() {
        return cmdsBuilder.build();
    }

    // A Commands builder methods
    public void purchaseMore(int whnum, Map<Product, Integer> products, long seqnum){
        APurchaseMore.Builder purchaseMore = APurchaseMore.newBuilder();
        purchaseMore.setWhnum(whnum);
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            purchaseMore.addThings(AProduct.newBuilder()
                                    .setId(entry.getKey().getId())
                                    .setDescription(entry.getKey().getDescription())
                                    .setCount(entry.getValue()));
        }
        purchaseMore.setSeqnum(seqnum);
        cmdsBuilder.addBuy(purchaseMore);
    }

    public void pack(int whnum, Map<Product, Integer> products, long shipid, long seqnum){
        APack.Builder toPack = APack.newBuilder();
        toPack.setWhnum(whnum);
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            toPack.addThings(AProduct.newBuilder()
                                    .setId(entry.getKey().getId())
                                    .setDescription(entry.getKey().getDescription())
                                    .setCount(entry.getValue()));
        }
        toPack.setShipid(shipid);
        toPack.setSeqnum(seqnum);
        cmdsBuilder.addTopack(toPack);
    }

    public void load(int whnum, int truckid, long shipid, long seqnum){
        APutOnTruck.Builder load = APutOnTruck.newBuilder();
        load.setWhnum(whnum);
        load.setTruckid(truckid);
        load.setShipid(shipid);
        load.setSeqnum(seqnum);
        cmdsBuilder.addLoad(load);
    }

    public void query(long packageid, long seqnum){
        AQuery.Builder query = AQuery.newBuilder();
        query.setPackageid(packageid);
        query.setSeqnum(seqnum);
        cmdsBuilder.addQueries(query);
    }

    public void setAcks(long[] acks){
        for (long ack : acks) {
            cmdsBuilder.addAcks(ack);
        }
    }

    public void setSimSpeed(int simSpeed){
        cmdsBuilder.setSimspeed(simSpeed);
    }

    public void disconnect(){
        cmdsBuilder.setDisconnect(true);
    }

    // A Response parser methods
    public List<Triplet<Integer, List<AProduct>, Long>> getArrived(AResponses responses){
        List<Triplet<Integer, List<AProduct>, Long>> arrivedProducts = new ArrayList<>();
        for(APurchaseMore arrived : responses.getArrivedList()){
            List<AProduct> products = new ArrayList<>();
            for(AProduct product : arrived.getThingsList()){
                products.add(product);
            }
            arrivedProducts.add(new Triplet<Integer, List<AProduct>, Long>(arrived.getWhnum(), products, arrived.getSeqnum()));
        }
        return arrivedProducts;
    }

    public Map<Long, Long> getPacked(AResponses responses){
        HashMap<Long, Long> packedShipIds = new HashMap<>();
        for(APacked packed : responses.getReadyList()){
            packedShipIds.put(packed.getShipid(), packed.getSeqnum());
        }
        return packedShipIds;
    }

    public Map<Long, Long> getLoaded(AResponses responses){
        HashMap<Long, Long> loadedShipIds = new HashMap<>();
        for(ALoaded loaded : responses.getLoadedList()){
            loadedShipIds.put(loaded.getShipid(), loaded.getSeqnum());
        }
        return loadedShipIds;
    }

    public boolean checkFinished(AResponses responses){
        return responses.getFinished();
    }

    public List<Triplet<String, Long, Long>> getErrs(AResponses responses){
        List<Triplet<String, Long, Long>> errs = new ArrayList<>();
        for(AErr err : responses.getErrorList()){
            errs.add(new Triplet<String, Long, Long>(err.getErr(), err.getOriginseqnum(), err.getSeqnum()));
        }
        return errs;
    }

    public List<Long> getAcks(AResponses responses){
        return responses.getAcksList();
    }

    public List<Triplet<Long, String, Long>> getStatus(AResponses responses){
        List<Triplet<Long, String, Long>> status = new ArrayList<>();
        for(APackage stat : responses.getPackagestatusList()){
            status.add(new Triplet<Long, String, Long>(stat.getPackageid(), stat.getStatus(), stat.getSeqnum()));
        }
        return status;
    }
}
