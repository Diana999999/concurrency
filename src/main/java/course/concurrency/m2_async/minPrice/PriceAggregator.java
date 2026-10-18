package course.concurrency.m2_async.minPrice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private final Set<Double> actualPrices = new ConcurrentSkipListSet<>();
    private static final Long GET_PRICE_TIMEOUT = 2980L;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {

        List<CompletableFuture<Double>> cfs = new ArrayList<>();
        shopIds.forEach(
                shopId -> cfs.add(
                        CompletableFuture
                                .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                                .orTimeout(GET_PRICE_TIMEOUT, TimeUnit.MILLISECONDS)
                                .exceptionally((ex) -> Double.NaN)
                )
        );
        cfs.forEach(cf -> actualPrices.add(cf.join()));

        return actualPrices.stream().sorted().findFirst().orElse(Double.NaN);
    }
}
