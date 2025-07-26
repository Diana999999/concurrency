package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    static final String ORDER_NOT_FOUND_TEMPLATE = "Order with id %s not found";
    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    private long nextId() {
        return nextId.incrementAndGet();
    }

    public long createOrder(List<Item> items) {
        Order order = currentOrders.computeIfAbsent(nextId(), orderId -> new Order(orderId, items));
        return order.getId();
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        Order updatedOrder = currentOrders.computeIfPresent(orderId, (id, currentOrder) -> currentOrder.withPaymentInfo(paymentInfo));
        throwNotFoundIfOrderIsNull(orderId, updatedOrder);
        if (updatedOrder.checkStatus()) {
            deliver(updatedOrder);
        }
    }

    public void setPacked(long orderId) {
        Order updatedOrder = currentOrders.computeIfPresent(orderId, (id, currentOrder) -> currentOrder.asPacked());
        throwNotFoundIfOrderIsNull(orderId, updatedOrder);
        if (updatedOrder.checkStatus()) {
            deliver(updatedOrder);
        }
    }

    private void deliver(Order order) {
        /* ... */
        currentOrders.computeIfPresent(order.getId(), (id, currentOrder) -> currentOrder.asDelivered());
    }

    public boolean isDelivered(long orderId) {
        Order order = currentOrders.get(orderId);
        throwNotFoundIfOrderIsNull(orderId, order);
        return order.getStatus().equals(Order.Status.DELIVERED);
    }

    private void throwNotFoundIfOrderIsNull(long orderId, Order order) {
        if (order == null) {
            throw new IllegalArgumentException(ORDER_NOT_FOUND_TEMPLATE.formatted(orderId));
        }
    }
}
