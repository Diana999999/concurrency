package course.concurrency.m3_shared.auction;

public interface Auction {

    boolean propose(Bid bid);

    Bid getLatestBid();
}
