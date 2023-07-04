package course.concurrency.exams.auction;

import org.junit.jupiter.api.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionStoppableTests {

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics stat = new ExecutionStatistics();

    private static final int iterations = 1_000_000;
    private static final int poolSize = Runtime.getRuntime().availableProcessors();
    private static final int bidCount = iterations * poolSize;

    private ExecutorService executor;
    private BlockingQueue<Long> priceQueue;
    private long latestPrice;
    private Notifier notifier;

    @BeforeEach
    public void setup() {
        notifier = new Notifier();

        executor = Executors.newFixedThreadPool(poolSize);
        priceQueue = new ArrayBlockingQueue<>(bidCount);
        for (long i = 0; i < bidCount/3; i++) {
            priceQueue.offer(i-1);
            priceQueue.offer(i);
            priceQueue.offer(i+1);
        }
        latestPrice = bidCount/3;
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
    public void testPessimistic() throws InterruptedException {
        AuctionStoppable pessimistic = new AuctionStoppablePessimistic(notifier);
        testCorrectLatestBid(pessimistic, "pessimistic");
    }

    @RepeatedTest(TEST_COUNT)
    public void testOptimistic() throws InterruptedException {
        AuctionStoppable optimistic = new AuctionStoppableOptimistic(notifier);
        testCorrectLatestBid(optimistic, "optimistic");
    }

    public void testCorrectLatestBid(AuctionStoppable auction, String tag) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                for (int it = 0; it < iterations; it++) {
                    long value = priceQueue.poll();
                    Bid bid = new Bid(value, value, value);
                    auction.propose(bid);
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        assertEquals(latestPrice, auction.getLatestBid().getPrice());
        stat.addData(tag,end - start);
    }

    @Test
    public void testStoppedAuctionPessimistic() throws InterruptedException {
        AuctionStoppable auction = new AuctionStoppablePessimistic(notifier);
        testStoppedAuction(auction);
    }

    @Test
    public void testStoppedAuctionOptimistic() throws InterruptedException {
        AuctionStoppable auction = new AuctionStoppableOptimistic(notifier);
        testStoppedAuction(auction);
    }

    public void testStoppedAuction(AuctionStoppable auction) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int priceToStop = iterations;
        AtomicReference<Bid> latestBidWhenStopped = new AtomicReference<>();

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                for (int it = 0; it < iterations; it++) {
                    long value = priceQueue.poll();
                    Bid bid = new Bid(value, value, value);
                    auction.propose(bid);
                    if (bid.getPrice() == priceToStop) {
                        Bid latest = auction.stopAuction();
                        latestBidWhenStopped.set(latest);
                    }
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(latestBidWhenStopped.get().getPrice(), auction.getLatestBid().getPrice());
    }
}
