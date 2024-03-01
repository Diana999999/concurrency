package course.concurrency.m3_shared.auction;

public interface AuctionStoppable extends Auction {

    // stop auction and return latest bid
    Bid stopAuction();
}
