package course.concurrency.m2_async.cf;

import org.springframework.scheduling.annotation.Async;

import java.util.stream.IntStream;

public class LoadGenerator {

    @Async
    public void work() {
//        sleep();
        compute();
    }

    private static void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int compute() {
        return IntStream.range(0, 5_000_000).boxed().filter(i -> i % 2 == 0).reduce((a, b) -> b).get();
    }
}