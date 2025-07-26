package course.concurrency.m3_shared.deadlock;

import java.util.concurrent.ConcurrentHashMap;

public class MapDeadlock {

    private static ConcurrentHashMap<String, String> map1 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> map2 = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        map1.put("key1", "value1");
        map2.put("key2", "value2");

        // Поток 1
        Thread thread1 = new Thread(() -> {
            System.out.println("Thread 1 started");
            map1.compute("key1", (k, v) -> {
                System.out.println("Thread 1 start merge");
                // Захватывает бакет для key1 в map1, затем пытается получить доступ к map2
                map2.merge("key2", "newValue", (oldVal, newVal) -> oldVal + newVal);
                return v;
            });
        });

        // Поток 2
        Thread thread2 = new Thread(() -> {
            System.out.println("Thread 2 started");
            map2.compute("key2", (k, v) -> {
                System.out.println("Thread 2 start merge");
                // Захватывает бакет для key2 в map2, затем пытается получить доступ к map1
                map1.merge("key1", "newValue", (oldVal, newVal) -> oldVal + newVal);
                return v;
            });
        });

        System.out.println("Starting");

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println("Finished");
    }
}
