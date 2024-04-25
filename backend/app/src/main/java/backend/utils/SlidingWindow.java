package backend.utils;

import java.util.LinkedList;

import backend.protocol.WorldAmazon.APackage;
import backend.protocol.AmazonUps.Err;
import backend.protocol.AmazonUps.UADelivered;
import backend.protocol.AmazonUps.UATruckArrived;
import backend.protocol.WorldAmazon.AErr;
import backend.protocol.WorldAmazon.ALoaded;
import backend.protocol.WorldAmazon.APacked;
import backend.protocol.WorldAmazon.APurchaseMore;

public class SlidingWindow {
    private LinkedList<Long> seqnums;
    private int maxSize;

    public SlidingWindow(int maxSize) {
        this.seqnums = new LinkedList<>();
        this.maxSize = maxSize;
    }

    public synchronized void addSeqnum(long seqnum) {
        seqnums.add(seqnum);
        if (seqnums.size() > maxSize) {
            seqnums.removeFirst();
        }
    }

    public synchronized boolean containsSeqnum(long seqnum) {
        return seqnums.contains(seqnum);
    }

    public synchronized void clearWindow() {
        seqnums.clear();
    }
}
