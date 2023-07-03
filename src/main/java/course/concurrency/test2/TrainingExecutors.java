package course.concurrency.test2;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TrainingExecutors {

    public static long longTask() throws InterruptedException {
        Thread.sleep(1000); // + try-catch
        return ThreadLocalRandom.current().nextInt();
    }

    public static void main(String[] args) {

        System.out.println(Runtime.getRuntime().availableProcessors());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        for (int i = 0; i < 1; i++) {
            executor.submit(TrainingExecutors::longTask);
            System.out.print(executor.getPoolSize() + " ");
        }

        executor.shutdown();
    }
}
