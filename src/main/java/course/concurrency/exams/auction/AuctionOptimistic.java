package course.concurrency.exams.auction;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(0L, 0L, 0L);

    public boolean propose(Bid bid) {
        if (bid.getPrice() > latestBid.getPrice()) {
            latestBid = bid;
            notifier.sendOutdatedMessage(bid);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {

        return latestBid;
    }
}
