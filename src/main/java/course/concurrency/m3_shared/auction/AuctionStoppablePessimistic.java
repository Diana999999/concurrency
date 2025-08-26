package course.concurrency.m3_shared.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private volatile boolean stopped = false;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid;

    public boolean propose(Bid bid) {
        if (bid.moreThen(latestBid)) {
            writeLock.lock();
            try {
                if (bid.moreThen(latestBid) && !stopped) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            } finally {
                writeLock.unlock();
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        try {
            readLock.lock();
            return latestBid;
        } finally {
            readLock.unlock();
        }
    }

    public Bid stopAuction() {
        this.stopped = true;
        return latestBid;
    }
}
