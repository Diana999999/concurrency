package course.concurrency.exams.auction;

public interface AuctionStoppable extends Auction {

    // stop auction and return latest bid
    Bid stopAuction();
}
