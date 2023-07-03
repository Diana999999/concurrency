package course.concurrency.m2_async.min_price;

import course.concurrency.m2_async.cf.min_price.PriceAggregator;
import course.concurrency.m2_async.cf.min_price.PriceRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PriceAggregatorTests {

    PriceAggregator priceAggregator;

    long randomItemId;

    private int SLA = 3000;

    @BeforeEach
    public void setup() {
        priceAggregator = new PriceAggregator();
        randomItemId = ThreadLocalRandom.current().nextLong();
    }

    @Test
    @DisplayName("Every shop responds")
    public void shouldReturnMin() {
        int shopCount = 50;
        PriceRetriever priceRetriever = mock(PriceRetriever.class);
        List<Double> prices = IntStream.range(0, shopCount-1).boxed()
                .map(i -> ThreadLocalRandom.current().nextDouble())
                .collect(toList());
        when(priceRetriever.getPrice(anyLong(), anyLong())).thenReturn(ThreadLocalRandom.current().nextDouble()*100,
                prices.toArray(new Double[]{}));
        priceAggregator.setPriceRetriever(priceRetriever);
        Set<Long> shops = LongStream.range(0, shopCount).boxed().collect(toSet());
        priceAggregator.setShops(shops);
        double expectedMin = prices.stream().min(Double::compareTo).get();

        long start = System.currentTimeMillis();
        double min = priceAggregator.getMinPrice(randomItemId);
        long end = System.currentTimeMillis();

        assertEquals(expectedMin, min, "Minimal price is evaluated incorrectly");
        assertTrue((end - start) < SLA, "Method evaluated too long");
    }

    @Test
    @DisplayName("No one responds")
    public void shouldReturnDefault() {
        PriceRetriever priceRetriever = mock(PriceRetriever.class);
        when(priceRetriever.getPrice(anyLong(), anyLong())).thenAnswer(inv -> {
            Thread.sleep(3000);
            return 12d;
        });
        priceAggregator.setPriceRetriever(priceRetriever);
        double expectedMin = Double.NaN;

        long start = System.currentTimeMillis();
        double min = priceAggregator.getMinPrice(15L);
        long end = System.currentTimeMillis();

        assertEquals(expectedMin, min, "Minimal price is evaluated incorrectly");
        assertTrue((end - start) < SLA, "Method evaluated too long");
    }

    @Test
    @DisplayName("Half of shops respond too long")
    public void shouldTolerateLongResponses(){
        int shopCount = 50;
        int half = shopCount/2;
        PriceRetriever priceRetriever = mock(PriceRetriever.class);
        List<Double> prices = IntStream.range(0, half).boxed()
                .map(i -> ThreadLocalRandom.current().nextDouble()*100).collect(toList());

        AtomicInteger totalRequestCount = new AtomicInteger();
        AtomicInteger succeedRequestCount = new AtomicInteger();

        when(priceRetriever.getPrice(anyLong(), anyLong())).thenAnswer(inv -> {
            if (totalRequestCount.getAndIncrement() < half) {
                Thread.sleep(SLA * 2);
                return null;
            } else {
                return prices.get(succeedRequestCount.getAndIncrement());
            }
        });

        priceAggregator.setPriceRetriever(priceRetriever);
        Set<Long> shops = LongStream.range(0, shopCount).boxed().collect(toSet());
        priceAggregator.setShops(shops);

        long start = System.currentTimeMillis();
        double min = priceAggregator.getMinPrice(randomItemId);
        long end = System.currentTimeMillis();

        double expectedMin = prices.stream().min(Double::compareTo).orElse(Double.NaN);
        assertEquals(expectedMin, min, "Minimal price is evaluated incorrectly");
        assertTrue((end - start) < SLA, "Method evaluated too long");
    }

    @Test
    @DisplayName("Not everyone responds")
    public void shouldReturnMinWithIncomplete() {
        int shopCount = 50;
        PriceRetriever priceRetriever = mock(PriceRetriever.class);
        AtomicBoolean isComplete = new AtomicBoolean(false);
        List<Double> prices = IntStream.range(0, shopCount).boxed()
                .map(i -> {
                    Double price = isComplete.get() ? ThreadLocalRandom.current().nextDouble()*100 : null;
                    isComplete.set(!isComplete.get());
                    return price;
                }).collect(toList());

        AtomicInteger index = new AtomicInteger();
        ConcurrentSkipListSet<Double> usedPrices = new ConcurrentSkipListSet<>();
        when(priceRetriever.getPrice(anyLong(), anyLong())).thenAnswer(inv -> {
            Double result = prices.get(index.getAndIncrement());
            if (result != null) {
                usedPrices.add(result);
                return result;
            } else {
                Thread.sleep(SLA);
                return null;
            }
        });

        priceAggregator.setPriceRetriever(priceRetriever);
        Set<Long> shops = LongStream.range(0, shopCount).boxed().collect(toSet());
        priceAggregator.setShops(shops);


        long start = System.currentTimeMillis();
        double min = priceAggregator.getMinPrice(randomItemId);
        long end = System.currentTimeMillis();

        double expectedMin = usedPrices.stream().min(Double::compareTo).orElse(Double.NaN);
        assertEquals(expectedMin, min, "Minimal price is evaluated incorrectly");
        assertTrue((end - start) < SLA, "Method evaluated too long");
    }

    @Test
    @DisplayName("Some shops respond with exception")
    public void shouldReturnMinWithException() {
        int shopCount = 50;
        PriceRetriever priceRetriever = mock(PriceRetriever.class);
        List<Double> prices = IntStream.range(0, shopCount).boxed()
                .map(i -> {
                    boolean isComplete = ThreadLocalRandom.current().nextBoolean();
                    if (isComplete) {
                        double price = ThreadLocalRandom.current().nextDouble()*100;
                        return price;
                    } else return null;
                }).collect(toList());

        AtomicInteger index = new AtomicInteger();
        when(priceRetriever.getPrice(anyLong(), anyLong())).thenAnswer(inv -> {
            Double result = prices.get(index.getAndIncrement());
            if (result != null) {
                return result;
            } else {
                throw new RuntimeException();
            }
        });

        priceAggregator.setPriceRetriever(priceRetriever);
        Set<Long> shops = LongStream.range(0, shopCount).boxed().collect(toSet());
        priceAggregator.setShops(shops);
        double expectedMin = prices.stream().filter(Objects::nonNull).min(Double::compareTo).get();

        long start = System.currentTimeMillis();
        double min = priceAggregator.getMinPrice(randomItemId);
        long end = System.currentTimeMillis();

        assertEquals(expectedMin, min, "Minimal price is evaluated incorrectly");
        assertTrue((end - start) < SLA, "Method evaluated too long");
    }

    @Test
    @DisplayName("Min price from last shop")
    public void shouldReturnMinFromLast() {
        int shopCount = 50;
        double expectedMinPrice = 10d;

        List<Long> shopIds = LongStream.range(0, shopCount-1).boxed().collect(toList());
        priceAggregator.setShops(shopIds);

        List<Double> prices = shopIds.stream().map(id -> 300+Double.valueOf(id)).collect(toList());
        prices.add(shopCount-1, expectedMinPrice);

        PriceRetriever priceRetriever = mock(PriceRetriever.class);
        AtomicInteger invocationCount = new AtomicInteger();
        when(priceRetriever.getPrice(anyLong(), anyLong())).thenAnswer(inv -> {
            int iteration = invocationCount.getAndIncrement();
            Thread.sleep(2500);
            return prices.get(iteration);
        });
        priceAggregator.setPriceRetriever(priceRetriever);
        Set<Long> shops = LongStream.range(0, shopCount).boxed().collect(toSet());
        priceAggregator.setShops(shops);

        long start = System.currentTimeMillis();
        double min = priceAggregator.getMinPrice(randomItemId);
        long end = System.currentTimeMillis();

        assertEquals(expectedMinPrice, min, "Minimal price is evaluated incorrectly");
        assertTrue((end - start) < SLA, "Method evaluated too long");
    }
}
