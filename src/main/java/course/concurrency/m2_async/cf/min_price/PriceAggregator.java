package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.*;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();


    //Кастомный компаратор для того чтобы NaN всегда располагался в конце списка
    private final Comparator<Double> comparator = (o1, o2) -> {
        if (Double.isNaN(o1) && Double.isNaN(o2)) {
            return 0;
        } else if (Double.isNaN(o1)) {
            return 1;
        } else if (Double.isNaN(o2)) {
            return -1;
        } else {
            return Double.compare(o1, o2);
        }
    };

    private final ConcurrentSkipListSet<Double> sortedSetOfReceivedPrices = new ConcurrentSkipListSet<>(comparator);

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        // place for your code
//        shopIds.stream().map(shopId -> getPriceFromOneSite(itemId, shopId)).forEach(e -> e.thenAcceptAsync(sortedSetOfReceivedPrices::add));
//        try {
//            Thread.currentThread().join(2950);
//        } catch (InterruptedException e) {
//            System.out.println("Exception");
//        }

        shopIds
                .stream()
                .parallel()
                .forEach(shopId -> CompletableFuture.runAsync(() -> getPriceFromOneSite(itemId, shopId)
                        .completeOnTimeout(Double.NaN, 3000, TimeUnit.MILLISECONDS)
                        .thenAccept(sortedSetOfReceivedPrices::add), executorService));
        try {
            Thread.currentThread().join(2995);
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception occurred");
        }
        return sortedSetOfReceivedPrices.first();
    }
    /*
    Надо написать метод
    где сначала будет выполняться работа в CompletableFuture c таймаутом
    передает также ConcurrentSkipListSet чтобы при завершении метода результат складывался туда
    далее спустя 2 с лишним секунды достаем первый элемент в ConcurrentSkipListSet
     */
    private CompletableFuture<Double> getPriceFromOneSite(long itemId, long shopId) {
        return CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId));
    }
}
