package course.concurrency.exams.auction;

public interface Auction {

    boolean propose(Bid bid);

    Bid getLatestBid();
}
