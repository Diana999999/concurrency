package course.concurrency.m2_async.minPrice;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private ExecutorService executorService = Executors.newFixedThreadPool(100);

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Double>[] futures = shopIds.stream()
                .map(shopId -> supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executorService)
                        .exceptionally(e -> Double.NaN)
                        .completeOnTimeout(Double.NaN, 2900, TimeUnit.MILLISECONDS)
                )
                .toArray(CompletableFuture[]::new);

        return allOf(futures)
                .thenApply(v -> getMin(futures))
                .completeOnTimeout(Double.NaN, 3000, TimeUnit.MILLISECONDS)
                .join();
    }

    private Double getMin(CompletableFuture<Double>[] futures) {
        return Arrays.stream(futures)
                .map(this::getSafe)
                .filter(d -> !Double.isNaN(d))
                .min(Double::compare)
                .orElse(Double.NaN);
    }

    private Double getSafe(CompletableFuture<Double> future) {
        try {
            return future.getNow(Double.NaN);
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
