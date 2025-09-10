import domain.OrderReceipt;
import exception.SoldOutException;
import org.junit.jupiter.api.Test;
import repo.ProductCatalog;
import service.CheckOutService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderTest {

    @Test
    void order_success_with_and_without_shippingFee() {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(catalog);

        Map<Long, Integer> req1 = new LinkedHashMap<>();
        req1.put(760709L, 10);
        OrderReceipt r1 = checkOutService.checkOut(req1);
        assertTrue(r1.getShippingFee() == 2500);
        assertTrue(r1.getPayAmount() == r1.getShippingFee() + r1.getOrderAmount());

        Map<Long, Integer> req2 = new LinkedHashMap<>();
        req2.put(377169L, 2); // 24,900
        req2.put(760709L, 1); //   200
        OrderReceipt r2 = checkOutService.checkOut(req2);
        assertTrue(r2.getOrderAmount() == 50000);
        assertTrue(r2.getShippingFee() == 0);
    }

    @Test
    void order_multi_items_success() {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(catalog);

        Map<Long, Integer> req1 = new LinkedHashMap<>();
        req1.put(782858L, 1); // 39500
        req1.put(760709L, 10); // 200 * 10 = 2000

        OrderReceipt r1 = checkOutService.checkOut(req1);
        assertTrue(r1.getItems().size() == 2);
        assertTrue(r1.getOrderAmount() == 41500);
        assertTrue(r1.getShippingFee() == 2500);
    }

    @Test
    void order_exact_stock_then_exceed_throw() {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(catalog);

        Map<Long, Integer> req1 = new LinkedHashMap<>();
        req1.put(648418L, 5); // 재고가 정확히 5개
        OrderReceipt r1 = checkOutService.checkOut(req1);
        assertTrue(r1.getOrderAmount() > 0);

        Map<Long, Integer> req2 = new LinkedHashMap<>();
        req2.put(648418L, 1);
        assertThrows(SoldOutException.class, () -> checkOutService.checkOut(req2));

    }

    @Test
    void rollback_on_failure() {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(catalog);


        long pid = 611019L; // 재고 7
        int before = catalog.findById(pid).get().getStock();

        Map<Long, Integer> bad = new LinkedHashMap<>();
        bad.put(pid, before + 10); // 초과 주문

        assertThrows(SoldOutException.class, () -> checkOutService.checkOut(bad));

        int after = catalog.findById(pid).get().getStock();
        assertTrue(after == before);

    }

    @Test
    void concurrent_orders_on_different_products_both_succeed() throws Exception {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService svc = new CheckOutService(catalog);

        long pid1 = 648418L; // 재고 5
        long pid2 = 611019L; // 재고 7

        Callable<Boolean> t1 = () -> {
            Map<Long,Integer> req = Map.of(pid1, 4);
            svc.checkOut(req);
            return true;
        };

        Callable<Boolean> t2 = () -> {
            Map<Long,Integer> req = Map.of(pid2, 6);
            svc.checkOut(req);
            return true;
        };

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService es = Executors.newFixedThreadPool(2);

        Future<Boolean> f1 = es.submit(() -> { start.await(); return t1.call(); });
        Future<Boolean> f2 = es.submit(() -> { start.await(); return t2.call(); });

        start.countDown();

        boolean r1 = f1.get();
        boolean r2 = f2.get();
        es.shutdown();

        // 같은 상품이 아니므로 동시에 성공해야 정상
        assertTrue(r1 && r2);
    }



    @Test
    void testOrderMultiThread_locking_ok() throws Exception {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService svc = new CheckOutService(catalog);

        long pid = 648418L; // 재고 5

        Callable<Boolean> task = () -> {
            Map<Long, Integer> req = new LinkedHashMap<>();
            req.put(pid, 4);
            try {
                svc.checkOut(req);
                System.out.println(Thread.currentThread().getName() + " -> SUCCESS");
                return true;
            } catch (SoldOutException e) {
                System.out.println(Thread.currentThread().getName() + " -> FAIL: " + e.getMessage());
                return false;
            }
        };

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService es = Executors.newFixedThreadPool(2);

        Future<Boolean> f1 = es.submit(() -> { start.await(); return task.call(); });
        Future<Boolean> f2 = es.submit(() -> { start.await(); return task.call(); });

        start.countDown();

        boolean r1 = f1.get();
        boolean r2 = f2.get();
        es.shutdown();

        int success = (r1 ? 1 : 0) + (r2 ? 1 : 0);
        int fail    = 2 - success;

        System.out.println("success=" + success + ", fail=" + fail);

        // 기대: 최소 한쪽은 실패 (보통 1 성공 / 1 실패)
        assertTrue(fail >= 1, "동시 주문에서 최소 한쪽은 SoldOutException이어야 합니다.");
    }
}
