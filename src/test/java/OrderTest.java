import domain.OrderReceipt;
import exception.SoldOutException;
import org.junit.jupiter.api.Test;
import repo.ProductCatalog;
import service.CheckOutService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.function.Try.call;

public class OrderTest {

    @Test
    void testOrderSurpass() {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(catalog);

        Map<Long, Integer> req = new LinkedHashMap<>();
        req.put(648418L, 6);

        assertThrows(SoldOutException.class, () -> checkOutService.checkOut(req));
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
