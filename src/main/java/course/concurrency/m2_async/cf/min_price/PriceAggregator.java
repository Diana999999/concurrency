package course.concurrency.m2_async.cf.min_price;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class PriceAggregator {
    private static final long TIMEOUT_VALUE = 2900L;

    private static final Double UNSUCCESSFUL_RESULT = Double.NaN;

    private static final Logger LOGGER = Logger.getLogger(PriceAggregator.class.getName());

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        ExecutorService executor = Executors.newFixedThreadPool(shopIds.size());
        List<CompletableFuture<Double>> completableFutureList =
                shopIds
                        .stream()
                        .map(shopId ->
                                CompletableFuture.supplyAsync(
                                                () -> priceRetriever.getPrice(itemId, shopId), executor)
                                        .completeOnTimeout(UNSUCCESSFUL_RESULT, TIMEOUT_VALUE, TimeUnit.MILLISECONDS)
                                        .exceptionally(e -> {
                                            LOGGER.log(Level.WARNING, e.getMessage());
                                            return UNSUCCESSFUL_RESULT;
                                        }))
                        .collect(toList());

        CompletableFuture
                .allOf(completableFutureList.toArray(CompletableFuture[]::new))
                .join();
        return completableFutureList
                .stream()
                .mapToDouble(CompletableFuture::join)
                .filter(d -> !Double.isNaN(d))
                .min()
                .orElse(UNSUCCESSFUL_RESULT);
    }
}
