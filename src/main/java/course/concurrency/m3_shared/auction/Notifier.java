package course.concurrency.m3_shared.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {

    //оптимальное количество потоков определено посредством тестов (выше/ниже - деградирует скорость)
    private final ExecutorService executorService = Executors.newFixedThreadPool(350);
    private volatile boolean isShutdown = false;

    public void sendOutdatedMessage(Bid bid) {
        executorService.submit(() -> {
            if (!isShutdown && !Thread.currentThread().isInterrupted()) {
                imitateSending(bid);
            }
        });
    }

    private void imitateSending(Bid bid) {
        // don't remove this delay, deal with it properly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
        this.isShutdown = true;
        this.executorService.shutdown();
    }
}
