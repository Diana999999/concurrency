package course.concurrency.m3_shared.auction;

import org.junit.jupiter.api.*;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionStoppableTests {

    @Suite
    @IncludeTags("pessimistic_stoppable")
    @SelectPackages("course.concurrency.m3_shared.auction")
    public static class PessimisticSuite {}

    @Suite
    @IncludeTags("optimistic_stoppable")
    @SelectPackages("course.concurrency.m3_shared.auction")
    public static class OptimisticSuite {}

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics stat = new ExecutionStatistics();

    private static final int iterations = 1_000_000;
    private static final int poolSize = Runtime.getRuntime().availableProcessors();

    private Notifier notifier;

    private AuctionStoppable pessimistic;
    private AuctionStoppable optimistic;

    // for stopAuction test
    private Supplier<AuctionStoppable> pessimisticSupplier;
    private Supplier<AuctionStoppable> optimisticSupplier;

    @BeforeEach
    public void setup() {
        notifier = new Notifier();

        pessimisticSupplier = () -> new AuctionStoppablePessimistic(notifier);
        pessimistic = pessimisticSupplier.get();

        optimisticSupplier = () -> new AuctionStoppableOptimistic(notifier);
        optimistic = optimisticSupplier.get();
    }

    @AfterEach
    public void tearDown() {
        notifier.shutdown();
    }

    @AfterAll
    public static void printStat() {
        stat.printStatistics();
    }

    @RepeatedTest(TEST_COUNT)
    @DisplayName("Pessimistic: load test")
    @Tag("pessimistic_stoppable")
    public void testPessimistic() throws InterruptedException {
        loadTest(pessimistic, "pessimistic");
    }

    @RepeatedTest(TEST_COUNT)
    @DisplayName("Optimistic: load test")
    @Tag("optimistic_stoppable")
    public void testOptimistic() throws InterruptedException {
        loadTest(optimistic, "optimistic");
    }

    public void loadTest(AuctionStoppable auction, String tag) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        BlockingQueue<Bid> bidQueue = new LinkedBlockingQueue<>();
        Bid latestBid = null;
        for (long i = 0; i < iterations*poolSize/3; i++) {
            bidQueue.offer(new Bid(i-1, i-1, i-1));
            bidQueue.offer(new Bid(i, i, i));
            latestBid = new Bid(i+1, i+1, i+1);
            bidQueue.offer(latestBid);
        }

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                for (int it = 0; it < iterations; it++) {
                    Bid bid = bidQueue.poll();
                    auction.propose(bid);
                    if (it % 5 == 0) {
                        auction.getLatestBid();
                    }
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        assertEquals(latestBid.getPrice(), auction.getLatestBid().getPrice());
        stat.addData(tag,end - start);
    }

    @Test
    @DisplayName("Pessimistic: stopAuction() test")
    @Tag("pessimistic_stoppable")
    public void shouldStopAuctionPessimistic() {
        testStoppedAuction(pessimistic);
    }

    @Test
    @DisplayName("Optimistic: stopAuction() test")
    @Tag("optimistic_stoppable")
    public void shouldStopAuction() {
        testStoppedAuction(optimistic);
    }

    public void testStoppedAuction(AuctionStoppable auction) {
        Bid expectedBid = new Bid(5L, 5L, 5L);

        boolean isOk = auction.propose(expectedBid);
        Bid latestBid = auction.getLatestBid();

        assertTrue(isOk, "Auction doesn't perform on single operation");
        assertEquals(expectedBid, latestBid);

        Bid latestAfterStop = auction.stopAuction();
        assertEquals(expectedBid, latestAfterStop, "Latest bid is not correct");

        boolean resAfterStop = auction.propose(new Bid(100l, 100l,  100l));
        assertFalse(resAfterStop, "Bids should not be accepted after stop");
        latestAfterStop = auction.stopAuction();
        assertEquals(expectedBid, latestAfterStop, "Latest bid is not correct");
    }

    @Test
    @DisplayName("Pessimistic: stopAuction works with data races")
    @Tag("pessimistic_stoppable")
    @Timeout(60)
    public void stopWithDataRacesPessimistic() throws InterruptedException {
        stopAuctionWithDataRaces(() -> pessimistic);
    }

    @Test
    @DisplayName("Optimistic: stopAuction works with data races")
    @Tag("optimistic_stoppable")
    @Timeout(60)
    public void stopWithDataRacesOptimistic() throws InterruptedException {
        stopAuctionWithDataRaces(() -> optimistic);
    }

    public void stopAuctionWithDataRaces(Supplier<AuctionStoppable> auctionSuppler) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        final AtomicReference<Bid> stoppedBid = new AtomicReference<>();
        Bid firstBid = new Bid(1L, 1L, 1L);

        Bid slow = new Bid(2l, 2l, 2l) {
            public Long getPrice() {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return super.getPrice();
            }
        };

        for (int i = 2; i < 100; i++) {
            AuctionStoppable auction = auctionSuppler.get();
            auction.propose(firstBid);
            stoppedBid.set(null);
            Bid raceBid = slow;

            CountDownLatch startAuctionTasksLatch = new CountDownLatch(1);
            CountDownLatch auctionTasksDoneLatch = new CountDownLatch(2);

            executor.submit(() -> {
                try {
                    startAuctionTasksLatch.await();
                } catch (InterruptedException ignored) {}
                auction.propose(raceBid);
                auctionTasksDoneLatch.countDown();
            });
            executor.submit(() -> {
                try {
                    startAuctionTasksLatch.await();
                } catch (InterruptedException ignored) {}
                Bid stopped = auction.stopAuction();
                stoppedBid.set(stopped);
                auctionTasksDoneLatch.countDown();
            });
            startAuctionTasksLatch.countDown();
            auctionTasksDoneLatch.await();

            Long latestPrice = auction.getLatestBid().getPrice();
            assertEquals(stoppedBid.get().getPrice(), latestPrice, "Bid was updated after stop");
        }

        executor.shutdownNow();
    }
}
