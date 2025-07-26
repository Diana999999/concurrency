package course.concurrency.m3_shared.testing;

import course.concurrency.m3_shared.auction.Bid;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestExperiments {

    private static final int iterations = 1000;
    private static final int poolSize = 10;

    // Don't change this class
    public static class Counter {
        private volatile int counter = 0;

        public void increment() {
            counter++;
        }

        public int get() {
            return counter;
        }
    }

    @RepeatedTest(100)
    public void counterShouldFail() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        Counter counter = new Counter();

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    counter.increment();
                } catch (InterruptedException ignored) {}
            });
        }
        latch.countDown();
        executor.shutdown();
        if(executor.awaitTermination(1, TimeUnit.MINUTES)){
            System.out.println("success shutdown");
        }

        assertEquals(iterations, counter.get());
    }
}
