package course.concurrency.m3_shared.auction;

import org.junit.jupiter.api.*;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import java.util.concurrent.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionTests {

    @Suite
    @IncludeTags("pessimistic")
    @SelectPackages("course.concurrency.m3_shared.auction")
    public static class PessimisticSuite {}

    @Suite
    @IncludeTags("optimistic")
    @SelectPackages("course.concurrency.m3_shared.auction")
    public static class OptimisticSuite {}

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics stat = new ExecutionStatistics();

    private static final int iterations = 1_000_000;
    private static final int poolSize = Runtime.getRuntime().availableProcessors();
    private static final int bidCount = iterations * poolSize;
    private Notifier notifier;
    private Auction pessimistic;
    private Auction optimistic;

    @BeforeEach
    public void setup() {
        notifier = new Notifier();
        pessimistic = new AuctionPessimistic(notifier);
        optimistic = new AuctionOptimistic
                (notifier);
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
    @DisplayName("Optimistic: load test")
    @Tag("optimistic")
    public void testOptimisticUnderLoad() throws InterruptedException {
        loadTest(optimistic, "optimistic");
    }

    @RepeatedTest(TEST_COUNT)
    @DisplayName("Pessimistic: load test")
    @Tag("pessimistic")
    public void testPessimisticUnderLoad() throws InterruptedException {
        loadTest(pessimistic, "pessimistic");
    }

    @Test
    @DisplayName("Optimistic: lost update because of data races")
    @Tag("optimistic")
    @Timeout(60)
    public void lostUpdateOptimistic() throws InterruptedException {
        shouldNotLoseUpdate(optimistic);
    }

    @Test
    @DisplayName("Pessimistic: lost update because of data races")
    @Tag("pessimistic")
    @Timeout(60)
    public void lostUpdatePessimistic() throws InterruptedException {
        shouldNotLoseUpdate(pessimistic);
    }

    public void loadTest(Auction auction, String tag) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch latch = new CountDownLatch(1);

        BlockingQueue<Bid> bidQueue = new LinkedBlockingQueue<>(bidCount);
        for (long i = 0; i < bidCount / 3; i++) {
            bidQueue.offer(new Bid(i - 1, i - 1, i - 1));
            bidQueue.offer(new Bid(i, i, i));
            bidQueue.offer(new Bid(i + 1, i + 1, i + 1));
        }
        long expectedPrice = bidCount / 3;

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
        executor.awaitTermination(20, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        assertEquals(expectedPrice, auction.getLatestBid().getPrice());
        stat.addData(tag, end - start);
    }

    public void shouldNotLoseUpdate(Auction auction) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        BlockingQueue<Bid> incBidQueue = new LinkedBlockingQueue<>();
        LongStream.range(0, iterations*2).boxed().forEach(i -> incBidQueue.offer(new Bid(i, i, i)));

        for (int i = 0; i < iterations; i++) {
            CountDownLatch startAuctionTasksLatch = new CountDownLatch(1);
            CountDownLatch auctionTasksDoneLatch = new CountDownLatch(2);
            Bid bid1 = incBidQueue.poll();
            Bid bid2 = incBidQueue.poll();

            executor.submit(() -> {
                try {
                    startAuctionTasksLatch.await();
                } catch (InterruptedException ignored) {}
                auction.propose(bid1);
                auctionTasksDoneLatch.countDown();
            });
            executor.submit(() -> {
                try {
                    startAuctionTasksLatch.await();
                } catch (InterruptedException ignored) {}
                auction.propose(bid2);
                auctionTasksDoneLatch.countDown();
            });
            startAuctionTasksLatch.countDown();
            auctionTasksDoneLatch.await();

            Long latestPrice = auction.getLatestBid().getPrice();
            Long expectedPrice = Math.max(bid1.getPrice(), bid2.getPrice());
            assertEquals(expectedPrice, latestPrice, "Lost update found");
        }

        executor.shutdownNow();
    }
}
