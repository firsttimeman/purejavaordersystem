package service;

import concurrent.SortedReentrantLockManager;
import exception.SoldOutException;
import org.junit.jupiter.api.Test;
import repo.ProductCatalog;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CheckOutServiceConcurrencyTest {


    @Test
    void 동시주문_단일상품_재고음수없고_성공건수는_초기재고만큼만() {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            // given
            ProductCatalog catalog = new ProductCatalog();
            CheckOutService service = new CheckOutService(catalog, new SortedReentrantLockManager());

            long productId = 1011L; // price=50000, stock=5
            int initialStock = catalog.findById(productId).orElseThrow().getStock();

            int threads = 50;
            ExecutorService pool = Executors.newFixedThreadPool(threads);

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            AtomicInteger success = new AtomicInteger();
            AtomicInteger soldOut = new AtomicInteger();
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

            // when
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await(); // 모두 여기서 대기하다가 동시에 출발
                        service.checkOut(Map.of(productId, 1));
                        success.incrementAndGet();
                    } catch (SoldOutException e) {
                        // 재고 부족은 정상 (초기 재고보다 더 많은 요청이 들어오므로)
                        soldOut.incrementAndGet();
                    } catch (Throwable t) {
                        // 다른 예외는 비정상
                        unexpected.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            done.await();
            pool.shutdownNow();

            // then
            assertTrue(unexpected.isEmpty(), "예상치 못한 예외: " + unexpected);

            // 성공 주문 수는 초기 재고만큼만 가능
            assertEquals(initialStock, success.get());

            // 나머지는 재고부족으로 실패하는 게 정상
            assertEquals(threads - initialStock, soldOut.get());

            // 최종 재고는 0이어야 함
            int finalStock = catalog.findById(productId).orElseThrow().getStock();
            assertEquals(0, finalStock);

            // 안전장치: 음수 재고는 절대 안 됨
            assertTrue(finalStock >= 0);
        });
    }


    @Test
    void 동시주문_두상품_반대순서여도_가능하다() {

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {

            ProductCatalog catalog = new ProductCatalog();
            CheckOutService service = new CheckOutService(catalog, new SortedReentrantLockManager());

            long id1 = 1004L;
            long id2 = 1005L;

            int threads = 40;
            ExecutorService pool = Executors.newFixedThreadPool(threads);

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            AtomicInteger success = new AtomicInteger();
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

            for(int i = 0; i < threads; i++) {

                boolean reverse = i % 2 == 1;

                pool.submit(() -> {
                    try {
                        start.await();

                        Map<Long, Integer> req = reverse ? Map.of(id2, 1, id1, 1) :
                                Map.of(id1, 1, id2, 1);

                        service.checkOut(req);
                        success.incrementAndGet();
                    } catch (Throwable t) {
                        unexpected.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            done.await();
            pool.shutdownNow();

            assertTrue(unexpected.isEmpty());
            assertEquals(threads, success.get());
        });
    }
}