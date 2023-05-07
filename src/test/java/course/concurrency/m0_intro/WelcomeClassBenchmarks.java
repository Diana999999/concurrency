package course.concurrency.m0_intro;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class WelcomeClassBenchmarks {

    private final WelcomeClass welcomeClass = new WelcomeClass();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WelcomeClassBenchmarks.class.getSimpleName())
                .warmupIterations(1)
                .measurementIterations(1)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public String simpleBenchmark() {
        return welcomeClass.getMessage();
    }
}
