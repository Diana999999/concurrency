package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid = new Bid(0L, 0L, 0L);
    private volatile boolean run = true;

    public synchronized boolean propose(Bid bid) {
        if (run && bid.getPrice() > latestBid.getPrice()) {
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
            return true;
        }
        return false;
    }

    public synchronized Bid getLatestBid() {
        return latestBid;
    }

    public synchronized Bid stopAuction() {
        this.run = false;
        return latestBid;
    }

    public void run(){
        this.run = true;
    }
}
