package course.concurrency.m3_shared.collections;

import course.concurrency.exams.auction.ExecutionStatistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestaurantServiceTests {

    private static final int TEST_COUNT = 10;
    private static final ExecutionStatistics stat = new ExecutionStatistics();

    private static final int iterations = 1_000_000;
    private static final int poolSize = Runtime.getRuntime().availableProcessors()*2;

    private ExecutorService executor;
    private RestaurantService service;

    @BeforeEach
    public void setup() {
        executor = Executors.newFixedThreadPool(poolSize);
        service = new RestaurantService();
    }

    @AfterAll
    public static void printStat() {
        stat.printStatistics();
    }

    @RepeatedTest(TEST_COUNT)
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {}

                for (int it = 0; it < iterations; it++) {
                    service.getByName("A");
                    service.getByName("B");
                    service.getByName("C");
                }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();

        Set<String> statResult = service.printStat();

        assertEquals(3, statResult.size());
        assertTrue(statResult.contains("A - " + iterations*poolSize));
        assertTrue(statResult.contains("B - " + iterations*poolSize));
        assertTrue(statResult.contains("C - " + iterations*poolSize));

        stat.addData("service",end - start);
    }
}
