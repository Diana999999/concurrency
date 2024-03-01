package course.concurrency.m3_shared.testing;

import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestExperiments {

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
    public void counterShouldFail() {
        int iterations = 5;

        Counter counter = new Counter();

        for (int i = 0; i < iterations; i++) {
            counter.increment();
        }

        assertEquals(iterations, counter.get());
    }
}
