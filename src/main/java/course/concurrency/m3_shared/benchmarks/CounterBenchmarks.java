package course.concurrency.m3_shared.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class CounterBenchmarks {

    // Change WRITERS and READERS to experiment
    public static final int WRITERS = 7;
    public static final int READERS = 1;

    private final AtomicLong atomicLongCounter = new AtomicLong();
    private final LongAdder longAdderCounter = new LongAdder();

    private final AtomicLong atomic = new AtomicLong();

    private final Lock lock = new ReentrantLock();
    private final Lock lockFair = new ReentrantLock(true);

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReadWriteLock readWriteLockFair = new ReentrantReadWriteLock(true);

    private final Semaphore semaphore = new Semaphore(1);
    private final Semaphore semaphoreFair = new Semaphore(1, true);

    private final StampedLock stampedLock = new StampedLock();

    private long value;
    private volatile long volatileValue;
    private long newValue;
    private long tmp = 0;

    @Setup
    public void setup() {
        tmp++;
        value = tmp;
        volatileValue = tmp;
        newValue = tmp+5;
        atomic.set(value);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(CounterBenchmarks.class.getName())
                .forks(1)
//                .resultFormat(ResultFormatType.JSON)
//                .result("benchmark-result.json")
                .build();

        new Runner(options).run();
    }

    @Benchmark
    @Group("AtomicLong_counter")
    @GroupThreads(WRITERS)
    public long writeAtomicLong() {
        return atomicLongCounter.incrementAndGet();
    }

    @Benchmark
    @Group("AtomicLong_counter")
    @GroupThreads(READERS)
    public long readAtomicLong() {
        return atomicLongCounter.get();
    }

    @Benchmark
    @Group("LongAdder_counter")
    @GroupThreads(WRITERS)
    public void writeLongAdder() {
        longAdderCounter.increment();
    }

    @Benchmark
    @Group("LongAdder_counter")
    @GroupThreads(READERS)
    public long readLongAdder() {
        return longAdderCounter.sum();
    }

    @Benchmark
    @Group("ReentrantLock")
    @GroupThreads(WRITERS)
    public void writeReentrantLock() {
        try {
            lock.lock();
            value++;
        } finally {
            lock.unlock();
        }
    }

    @Benchmark
    @Group("ReentrantLock")
    @GroupThreads(READERS)
    public long readReentrantLock() {
        try {
            lock.lock();
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Benchmark
    @Group("ReentrantLock_fair")
    @GroupThreads(WRITERS)
    public void writeReentrantLockFair() {
        try {
            lockFair.lock();
            value++;
        } finally {
            lockFair.unlock();
        }
    }

    @Benchmark
    @Group("ReentrantLock_fair")
    @GroupThreads(READERS)
    public long readReentrantLockFair() {
        try {
            lockFair.lock();
            return value;
        } finally {
            lockFair.unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock")
    @GroupThreads(WRITERS)
    public void writeReadWriteLock() {
        try {
            readWriteLock.writeLock().lock();
            value++;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock")
    @GroupThreads(READERS)
    public long readReadWriteLock() {
        try {
            readWriteLock.readLock().lock();
            return value;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock_fair")
    @GroupThreads(WRITERS)
    public void writeReadWriteLockFair() {
        try {
            readWriteLockFair.writeLock().lock();
            value++;
        } finally {
            readWriteLockFair.writeLock().unlock();
        }
    }

    @Benchmark
    @Group("ReadWriteLock_fair")
    @GroupThreads(READERS)
    public long readReadWriteLockFair() {
        try {
            readWriteLockFair.readLock().lock();
            return value;
        } finally {
            readWriteLockFair.readLock().unlock();
        }
    }

    @Benchmark
    @Group("Semaphore")
    @GroupThreads(WRITERS)
    public void writeSemaphore() throws InterruptedException {
        try {
            semaphore.acquire();
            value++;
        } finally {
            semaphore.release();
        }
    }

    @Benchmark
    @Group("Semaphore")
    @GroupThreads(READERS)
    public long readSemaphore() throws InterruptedException {
        try {
            semaphore.acquire();
            return value;
        } finally {
            semaphore.release();
        }
    }

    @Benchmark
    @Group("Semaphore_fair")
    @GroupThreads(WRITERS)
    public void writeSemaphoreFair() throws InterruptedException {
        try {
            semaphoreFair.acquire();
            value++;
        } finally {
            semaphoreFair.release();
        }
    }

    @Benchmark
    @Group("Semaphore_fair")
    @GroupThreads(READERS)
    public long readSemaphoreFair() throws InterruptedException {
        try {
            semaphoreFair.acquire();
            return value;
        } finally {
            semaphoreFair.release();
        }
    }

    @Benchmark
    @Group("StampedLock_readwrite")
    @GroupThreads(WRITERS)
    public void writeStampedLockRW() {
        long stamp = stampedLock.writeLock();
        try {
            value++;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Benchmark
    @Group("StampedLock_readwrite")
    @GroupThreads(READERS)
    public long readStampedLockRW() {
        long stamp = stampedLock.readLock();
        try {
            return value;
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }

    @Benchmark
    @Group("StampedLock_optimistic")
    @GroupThreads(WRITERS)
    public void writeStampedLockOptimistic() {
        long stamp = stampedLock.writeLock();
        try {
            value++;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Benchmark
    @Group("StampedLock_optimistic")
    @GroupThreads(READERS)
    public long readStampedLockOptimistic() {
        long stamp;
        do {
            stamp = stampedLock.tryOptimisticRead();
            value = newValue;
        } while (!stampedLock.validate(stamp));
        return value;
    }

    @Benchmark
    @Group("synchronized")
    @GroupThreads(WRITERS)
    public void writeSynchronized() {
        synchronized (this) {
            value++;
        }
    }

    @Benchmark
    @Group("synchronized")
    @GroupThreads(READERS)
    public long readSynchronized() {
        synchronized (this) {
            return value;
        }
    }

    @Benchmark
    @Group("volatile_synchronized")
    @GroupThreads(WRITERS)
    public void writeSynchronizedVolatile() {
        synchronized (this) {
            volatileValue++;
        }
    }

    @Benchmark
    @Group("volatile_synchronized")
    @GroupThreads(READERS)
    public long readSynchronizedVolatile() {
        return volatileValue;
    }

    @Benchmark
    @Group("volatile")
    @GroupThreads(WRITERS)
    public void writeVolatile() {
        volatileValue = newValue;
    }

    @Benchmark
    @Group("volatile")
    @GroupThreads(READERS)
    public long readVolatile() {
        return volatileValue;
    }

    @Benchmark
    @Group("Atomic")
    @GroupThreads(READERS)
    public long readAtomic() {
        return atomic.get();
    }

    @Benchmark
    @Group("Atomic")
    @GroupThreads(WRITERS)
    public void writeAtomic() {
        atomic.set(newValue);
    }

    @Benchmark
    @Group("Atomic_optimistic")
    @GroupThreads(WRITERS)
    public long updateOptimistic() {
        long previous, tmp;
        do {
            previous = atomic.get();
            tmp = previous + newValue;
        } while (!atomic.compareAndSet(previous, newValue));
        return newValue;
    }

    @Benchmark
    @Group("Atomic_accumulate")
    @GroupThreads(WRITERS)
    public long updateAtomicMethods() {
        return atomic.accumulateAndGet(newValue, (x1, x2) -> x1 + x2);
    }
}
