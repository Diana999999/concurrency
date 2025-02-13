package course.concurrency.m2_async.minPrice;

import java.util.concurrent.ThreadLocalRandom;

public class PriceRetriever {

    public Double getPrice(long itemId, long shopId) {
        int delay = ThreadLocalRandom.current().nextInt(10);
        sleep(delay);
        return ThreadLocalRandom.current().nextDouble(1000);
    }

    private void sleep(int delay) {
        try { Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {}
    }
}
