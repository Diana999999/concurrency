package course.concurrency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class CustomBlockingQueueTest {

    private static final int poolSize = 10;

    @RepeatedTest(100)
    void enqueue() throws InterruptedException {
        int size = 100;
        CustomBlockingQueue<Integer> blockingQueue = new CustomBlockingQueue<>(size);
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < size; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    blockingQueue.enqueue(finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean successShutdown = executor.awaitTermination(5, TimeUnit.SECONDS);

        Assertions.assertTrue(successShutdown);
        Assertions.assertEquals(size, blockingQueue.size());
    }

    @RepeatedTest(100)
    void enqueueBlockIfIsSizeLimit() throws InterruptedException {
        int size = 100;
        CustomBlockingQueue<Integer> blockingQueue = new CustomBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            blockingQueue.enqueue(i);
        }

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < size; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    blockingQueue.enqueue(finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean successShutdown = executor.awaitTermination(100, TimeUnit.MILLISECONDS);

        Assertions.assertFalse(successShutdown);
        Assertions.assertEquals(size, blockingQueue.size());
    }

    @RepeatedTest(100)
    void dequeue() throws InterruptedException {
        int size = 100;
        CustomBlockingQueue<Integer> blockingQueue = new CustomBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            blockingQueue.enqueue(i);
        }
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < size; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    blockingQueue.dequeue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean successShutdown = executor.awaitTermination(5, TimeUnit.SECONDS);

        Assertions.assertTrue(successShutdown);
        Assertions.assertEquals(0, blockingQueue.size());
    }

    @RepeatedTest(100)
    void dequeueBlockIfIsEmpty() throws InterruptedException {
        int size = 100;
        CustomBlockingQueue<Integer> blockingQueue = new CustomBlockingQueue<>(size);
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < size; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    blockingQueue.dequeue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean successShutdown = executor.awaitTermination(100, TimeUnit.MILLISECONDS);

        Assertions.assertFalse(successShutdown);
    }

    @RepeatedTest(100)
    void hasNoDeadlocks() throws InterruptedException {
        int size = 100;
        CustomBlockingQueue<Integer> blockingQueue = new CustomBlockingQueue<>(size);

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize*2);
        for (int i = 0; i < size; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    blockingQueue.enqueue(finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        for (int i = 0; i < size; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    blockingQueue.dequeue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean successShutdown = executor.awaitTermination(5, TimeUnit.SECONDS);

        Assertions.assertTrue(successShutdown);
        Assertions.assertEquals(0, blockingQueue.size());
    }
}