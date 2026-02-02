package service;

import concurrent.NoOpLockManager;
import concurrent.ProductLockManager;
import concurrent.SortedReentrantLockManager;
import exception.SoldOutException;
import org.junit.jupiter.api.Test;
import repo.ProductCatalog;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CheckOutServiceBeforeAfterComparisonTest {

    @Test
    void 동시주문_락_전후_비교_같은시나리오로_돌린다() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {

            long productId = 1011L;   // stock=5
            int threads = 200;        // 동시 요청 수
            int loops = 30;           // 락 없는 케이스는 확률이라 반복 필요

            Result before = runScenario("BEFORE(NoOp)", new NoOpLockManager(), productId, threads, loops);
            Result after  = runScenario("AFTER(Sorted)", new SortedReentrantLockManager(), productId, threads, loops);

            System.out.println("==== SUMMARY ====");
            System.out.println(before.summary());
            System.out.println(after.summary());


            assertEquals(0, after.brokenRounds, "락 적용인데도 깨진 라운드가 있으면 안 됨");
            assertEquals(loops, after.okRounds);


            assertTrue(before.okRounds + before.brokenRounds == loops);
        });
    }

    private Result runScenario(String label,
                               ProductLockManager lockManager,
                               long productId,
                               int threads,
                               int loops) throws Exception {

        int ok = 0;
        int broken = 0;

        for (int round = 1; round <= loops; round++) {
            ProductCatalog catalog = new ProductCatalog();
            CheckOutService service = new CheckOutService(catalog, lockManager);

            int initialStock = catalog.findById(productId).orElseThrow().getStock();

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            AtomicInteger success = new AtomicInteger();
            AtomicInteger soldOut = new AtomicInteger();
            ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

            long t0 = System.nanoTime();

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        service.checkOut(Map.of(productId, 1));
                        success.incrementAndGet();
                    } catch (SoldOutException e) {
                        soldOut.incrementAndGet();
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

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            int finalStock = catalog.findById(productId).orElseThrow().getStock();

            boolean isBroken =
                    !unexpected.isEmpty()
                    || success.get() > initialStock   // 초과 성공
                    || finalStock < 0;                // 음수 재고

            if (isBroken) broken++;
            else ok++;


            System.out.printf(
                    "[%s][round=%02d] initial=%d, success=%d, soldOut=%d, final=%d, unexpected=%d, time=%dms%s%n",
                    label, round, initialStock, success.get(), soldOut.get(), finalStock, unexpected.size(), elapsedMs,
                    isBroken ? "  <-- BROKEN" : ""
            );
        }

        return new Result(label, loops, ok, broken);
    }

    private static class Result {
        final String label;
        final int loops;
        final int okRounds;
        final int brokenRounds;

        Result(String label, int loops, int okRounds, int brokenRounds) {
            this.label = label;
            this.loops = loops;
            this.okRounds = okRounds;
            this.brokenRounds = brokenRounds;
        }

        String summary() {
            return String.format("%s: ok=%d/%d, broken=%d/%d",
                    label, okRounds, loops, brokenRounds, loops);
        }
    }
}