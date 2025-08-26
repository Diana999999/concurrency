package course.concurrency.m3_shared.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicMarkableReference<Bid> latestBidRef = new AtomicMarkableReference<>(null, false);

    public boolean propose(Bid bid) {
        Bid latestBid;
        do {
            latestBid = latestBidRef.getReference();
            if (!bid.moreThen(latestBid) || latestBidRef.isMarked()) {
                return false;
            }

        } while (!latestBidRef.compareAndSet(latestBid, bid, false, false));

        notifier.sendOutdatedMessage(latestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBidRef.getReference();
    }

    public Bid stopAuction() {
        Bid latestBid;
        do {
            latestBid = latestBidRef.getReference();
        } while (!latestBidRef.attemptMark(latestBid, true));
        return latestBid;
    }
}
