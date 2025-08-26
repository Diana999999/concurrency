package course.concurrency.m6_streams;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTask {

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        return new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                1000,
                60L,
                TimeUnit.SECONDS,
                new OfferToStartLinkedBlockingQueue()
        );
    }

    static class OfferToStartLinkedBlockingQueue extends LinkedBlockingDeque<Runnable> {

        @Override
        public boolean offer(Runnable e) {
            return super.offerFirst(e);
        }
    }

    // Task #2
    public ThreadPoolExecutor getRejectExecutor() {
        return new ThreadPoolExecutor(
                8,
                8,
                60L,
                TimeUnit.SECONDS,
                new EmptyArrayBlockingQueue(),
                (r, executor) -> System.out.println("Rejected task: " + r)
        );
    }

    static class EmptyArrayBlockingQueue extends ArrayBlockingQueue<Runnable> {
        public EmptyArrayBlockingQueue() {
            super(1);
        }

        @Override
        public boolean offer(Runnable e) {
            return false;
        }
    }
}
