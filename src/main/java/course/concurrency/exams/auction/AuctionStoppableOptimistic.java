package course.concurrency.exams.auction;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(0L, 0L, 0L);
    private volatile boolean run = true;

    public boolean propose(Bid bid) {
        if (run && bid.getPrice() > latestBid.getPrice()) {
            latestBid = bid;
            notifier.sendOutdatedMessage(latestBid);
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        this.run = false;
        return latestBid;
    }

    public void run(){
        this.run = true;
    }
}
