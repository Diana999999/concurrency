package course.concurrency.m3_shared.immutable;

import java.util.ArrayList;
import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public class Order {

    public enum Status {NEW, IN_PROGRESS, DELIVERED}

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public Order(Long id, List<Item> items) {
        this(id, items, null, false, NEW);
    }

    private Order(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.id = id;
        this.items = new ArrayList<>(items);
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        if (paymentInfo == null) {
            throw new IllegalArgumentException("Payment info must not be null");
        }
        return new Order(this.id, this.items, paymentInfo, this.isPacked, Status.IN_PROGRESS);
    }

    public Order withPacked(boolean isPacked) {
        return new Order(this.id, this.items, this.paymentInfo, isPacked, Status.IN_PROGRESS);
    }

    public Order asDelivered() {
        return withStatus(Status.DELIVERED);
    }

    public Order withStatus(Status status) {
        return new Order(this.id, this.items, this.paymentInfo, this.isPacked, status);
    }

    public Order asPacked() {
        return withPacked(true);
    }

    public boolean checkStatus() {
        return paymentInfo != null && isPacked;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Status getStatus() {
        return status;
    }
}
