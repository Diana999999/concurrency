package course.concurrency.m3_shared.immutable;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderServiceTests {

    private OrderService service = new OrderService();

    private List<Item> items = List.of(new Item(), new Item());

    @Test
    public void testDelivery() {
        long orderId = service.createOrder(items);
        service.setPacked(orderId);
        service.updatePaymentInfo(orderId, new PaymentInfo());

        boolean isDelivered = service.isDelivered(orderId);
        assertTrue(isDelivered);
    }

    @Test
    public void testDeliveryWithDuplicatePacking() {
        long orderId = service.createOrder(items);
        service.setPacked(orderId);
        service.setPacked(orderId);
        service.setPacked(orderId);
        service.updatePaymentInfo(orderId, new PaymentInfo());

        boolean isDelivered = service.isDelivered(orderId);
        assertTrue(isDelivered);
    }

    @Test
    public void testPartialCompleteWithDelivery() {
        long orderId = service.createOrder(items);
        service.updatePaymentInfo(orderId, new PaymentInfo());

        boolean isDelivered = service.isDelivered(orderId);
        assertFalse(isDelivered);
    }

    @Test
    public void testPartialCompleteWithPacking() {
        long orderId = service.createOrder(items);
        service.setPacked(orderId);

        boolean isDelivered = service.isDelivered(orderId);
        assertFalse(isDelivered);
    }

    @Test
    public void testPartialCompleteWith2Packing() {
        long orderId = service.createOrder(items);
        service.setPacked(orderId);
        service.setPacked(orderId);

        boolean isDelivered = service.isDelivered(orderId);
        assertFalse(isDelivered);
    }

    @RepeatedTest(5)
    public void testWithManyThreads() throws InterruptedException {
        int iterations = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        BlockingQueue<Long> orderToPack = new ArrayBlockingQueue<>(iterations*3);
        BlockingQueue<Long> orderToPay = new ArrayBlockingQueue<>(iterations);
        BlockingQueue<Long> orderIdLog = new ArrayBlockingQueue<>(iterations);

        for(int i = 0; i< iterations; ++i) {
            Long id = service.createOrder(items);
                        orderToPack.offer(id);
                        orderToPay.offer(id);
                        orderIdLog.offer(id);
        }

        for(int i = 0; i< iterations; ++i) {
            executor.submit(() -> service.setPacked(orderToPack.poll()));
            executor.submit(() -> service.updatePaymentInfo(orderToPay.poll(), new PaymentInfo()));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        do {
            long orderId = orderIdLog.take();
            boolean isDelivered = service.isDelivered(orderId);
            assertTrue(isDelivered, String.format("Order %d is not completed", orderId));
        } while (!orderIdLog.isEmpty());
    }
}
