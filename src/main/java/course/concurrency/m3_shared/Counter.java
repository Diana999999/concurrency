package course.concurrency.m3_shared;

public class Counter {

    public static void first() {

    }

    public static void second() {

    }

    public static void third() {

    }

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> first());
        Thread t2 = new Thread(() -> second());
        Thread t3 = new Thread(() -> third());
        t1.start();
        t2.start();
        t3.start();
    }
}
