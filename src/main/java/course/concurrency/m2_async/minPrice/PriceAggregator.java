package course.concurrency.m2_async.minPrice;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PriceAggregator {

    public static final List<Double> mynext = new ArrayList<>(Arrays.asList(Double.NaN));

    private static PriceRetriever priceRetriever = new PriceRetriever();

    private ExecutorService executor = Executors.newCachedThreadPool();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private static Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        List<CompletableFuture<Double>>shops= shopIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, id),executor)
                        .completeOnTimeout(Double.POSITIVE_INFINITY, 2900l, TimeUnit.MILLISECONDS)
                        .exceptionally(ex->Double.POSITIVE_INFINITY)
                )
                .collect(Collectors.toList());

        CompletableFuture.allOf(shops.toArray(CompletableFuture[]::new)).join();

        return shops.stream().mapToDouble(CompletableFuture::join).filter(Double::isFinite)
                .min().orElse(Double.NaN);

    }
}
