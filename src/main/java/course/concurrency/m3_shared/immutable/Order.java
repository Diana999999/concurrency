package course.concurrency.m3_shared.immutable;

import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    private Long id;
    private List<Item> items;
    private PaymentInfo paymentInfo;
    private boolean isPacked;
    private Status status;

    public Order(List<Item> items) {
        this.items = items;
        this.status = NEW;
    }

    public synchronized boolean checkStatus() {
        if (items != null && !items.isEmpty() && paymentInfo != null && isPacked) {
            return true;
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public void setPaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
        this.status = Status.IN_PROGRESS;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public void setPacked(boolean packed) {
        isPacked = packed;
        this.status = Status.IN_PROGRESS;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
