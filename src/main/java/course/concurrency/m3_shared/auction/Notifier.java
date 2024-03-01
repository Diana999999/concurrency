package course.concurrency.m3_shared.auction;

public class Notifier {

    public void sendOutdatedMessage(Bid bid) {
        imitateSending();
    }

    private void imitateSending() {
        // don't remove this delay, deal with it properly
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {}
}
