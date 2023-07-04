package course.concurrency.exams.auction;

import org.junit.jupiter.api.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionTests {

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics stat = new ExecutionStatistics();

    private static final int iterations = 1_000_000;
    private static final int poolSize = Runtime.getRuntime().availableProcessors();
    private static final int bidCount = iterations * poolSize;

    private ExecutorService executor;
    private BlockingQueue<Long> priceQueue;
    private long expectedPrice;
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
        expectedPrice = bidCount/3;
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
    public void testOptimistic() throws InterruptedException {
        Auction auction = new AuctionOptimistic(notifier);
        testCorrectLatestBid(auction, "optimistic");
    }

    @RepeatedTest(TEST_COUNT)
    public void testPessimistic() throws InterruptedException {
        Auction auction = new AuctionPessimistic(notifier);
        testCorrectLatestBid(auction, "pessimistic");
    }

    public void testCorrectLatestBid(Auction auction, String tag) throws InterruptedException {
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
                    if (it % 200 == 0) {
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
        stat.addData(tag,end - start);
    }
}
