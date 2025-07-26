package course.concurrency.m3_shared.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBidRef = new AtomicReference<>();

    public boolean propose(Bid bid) {
        Bid latestBid;
        do {
            latestBid = latestBidRef.get();
            if (!bid.moreThen(latestBid)) {
                return false;
            }
        } while (!latestBidRef.compareAndSet(latestBid, bid));
        notifier.sendOutdatedMessage(latestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBidRef.get();
    }
}
