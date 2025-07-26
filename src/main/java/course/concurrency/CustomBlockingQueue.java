package course.concurrency;

import java.util.ArrayDeque;
import java.util.Queue;

public class CustomBlockingQueue<T> {

    private final int capacity;
    private final Queue<T> queue;
    private final Object lock = new Object();
    private int size = 0;

    public CustomBlockingQueue(int capacity) {
        this.queue = new ArrayDeque<>(capacity);
        this.capacity = capacity;
    }

    public void enqueue(T value) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == capacity) {
                lock.wait();
            }
            queue.add(value);
            size++;
            lock.notifyAll();
        }
    }

    public T dequeue() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                lock.wait();
            }
            T res = queue.poll();
            size--;
            lock.notifyAll();
            return res;
        }
    }

    public int size() {
        synchronized (lock) {
            return size;
        }
    }

    public int capacity() {
        return capacity;
    }
}
