import domain.Product;
import org.junit.jupiter.api.Test;
import repo.ProductCatalog;
import service.CheckOutService;
import service.SynchronizedCheckOutService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyTest {

    @Test
    void same_product_multi_thread_only_stock_size_success() throws ExecutionException, InterruptedException {

        //given
        ProductCatalog productCatalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(productCatalog);

        long pid = 1011L;
        Product product = productCatalog.findById(pid).orElseThrow();
        int stock = product.getStock();

        int threadCount = 10;
        int orderQuantityPerThread = 1;
        int exceptedMaxSuccess = stock / orderQuantityPerThread;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);


        List<Future<Boolean>> futures = new ArrayList<>();
        for(int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                start.await();

                Map<Long, Integer> req = Map.of(pid, orderQuantityPerThread);
                try {
                    checkOutService.checkOut(req);
                    System.out.println(Thread.currentThread().getName() + "-> SUCCESS");
                    return true;
                }catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " -> FAIL: " + e.getMessage());
                    return false;
                }
            }));
        }

        start.countDown();

        int success = 0;
        int fail = 0;

        for (Future<Boolean> future : futures) {
            if(future.get()) success++;
            else fail++;
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        int finalStock = product.getStock();

        System.out.println("stock = " + stock);
        System.out.println("success = " + success + ", fail = " + fail);
        System.out.println("finalStock = " + finalStock);

        // 1) 재고는 절대 음수가 되면 안 됨
        assertTrue(finalStock >= 0, "재고가 음수가 되면 안 됩니다.");

        // 2) 실제로 감소한 재고량 == (성공한 주문 수 * 주문 수량)
        assertEquals(stock - finalStock,
                success * orderQuantityPerThread,
                "재고 감소량과 성공 주문 수가 일치해야 합니다.");

        // 3) 성공한 주문 수는 애초 재고로 가능한 최대 수를 넘을 수 없음
        assertTrue(success <= exceptedMaxSuccess,
                "성공한 주문 수가 재고보다 많을 수 없습니다.");

    }


    @Test
    void concurrent_orders_on_different_products_both_success() throws ExecutionException, InterruptedException {
        ProductCatalog productCatalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(productCatalog);

        long pid1 = 1001L;
        long pid2 = 1002L;

        Callable<Boolean> t1 = () -> {
            Map<Long,Integer> req = Map.of(pid1, 5);
            checkOutService.checkOut(req);
            return true;
        };

        Callable<Boolean> t2 = () -> {
            Map<Long,Integer> req = Map.of(pid2, 3);
            checkOutService.checkOut(req);
            return true;
        };

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService es = Executors.newFixedThreadPool(2);


        Future<Boolean> f1 = es.submit(() -> { start.await(); return t1.call(); });
        Future<Boolean> f2 = es.submit(() -> { start.await(); return t2.call(); });


        // 동시에 출발
        start.countDown();

        boolean r1 = f1.get();
        boolean r2 = f2.get();
        es.shutdown();

        // 서로 다른 상품이므로 둘 다 성공해야 정상
        assertTrue(r1 && r2);
    }


    @Test
    void same_product_multi_thread_only_stock_size_success_sync() throws ExecutionException, InterruptedException {

        // given
        ProductCatalog productCatalog = new ProductCatalog();
        SynchronizedCheckOutService checkOutService = new SynchronizedCheckOutService(productCatalog);

        long pid = 1011L;
        Product product = productCatalog.findById(pid).orElseThrow();
        int stock = product.getStock();

        int threadCount = 10;
        int orderQuantityPerThread = 1;
        int expectedMaxSuccess = stock / orderQuantityPerThread;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                start.await();
                Map<Long, Integer> req = Map.of(pid, orderQuantityPerThread);
                try {
                    checkOutService.checkOut(req);
                    System.out.println(Thread.currentThread().getName() + " -> SUCCESS");
                    return true;
                } catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + " -> FAIL: " + e.getMessage());
                    return false;
                }
            }));
        }

        start.countDown();

        int success = 0;
        int fail = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) success++;
            else fail++;
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        int finalStock = product.getStock();

        System.out.println("=== SYNCHRONIZED TEST ===");
        System.out.println("initial stock = " + stock);
        System.out.println("success = " + success + ", fail = " + fail);
        System.out.println("finalStock = " + finalStock);

        assertTrue(finalStock >= 0);
        assertEquals(stock - finalStock, success * orderQuantityPerThread);
        assertTrue(success <= expectedMaxSuccess);
    }
}
