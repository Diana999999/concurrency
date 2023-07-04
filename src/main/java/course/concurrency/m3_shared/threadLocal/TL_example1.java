package course.concurrency.m3_shared.threadLocal;

public class TL_example1 {

    public static class Task implements Runnable {
        private static final ThreadLocal<Integer> value = ThreadLocal.withInitial(() -> 0);

        @Override
        public void run() {
            Integer currentValue = value.get();
            value.set(currentValue + 1);
            System.out.print(value.get() + " ");
        }
    }

    public static void main(String[] args) {
        Task task1 = new Task();

        new Thread(task1).start();
        new Thread(task1).start();
        new Thread(task1).start();
    }
}
