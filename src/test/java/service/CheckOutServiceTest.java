package service;

import concurrent.SortedReentrantLockManager;
import domain.OrderReceipt;
import exception.SoldOutException;
import org.junit.jupiter.api.Test;
import repo.ProductCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckOutServiceTest {

    @Test
    void checkOut_정사주문_재고차감_영수증금액계산() {
        ProductCatalog productCatalog = new ProductCatalog();
        CheckOutService service = new CheckOutService(productCatalog, new SortedReentrantLockManager());
        long id = 1011L;
        int before = productCatalog.findById(id).orElseThrow().getStock();

        Map<Long, Integer> req = new LinkedHashMap<>();
        req.put(id, 1);

        OrderReceipt receipt = service.checkOut(req);
        int after = productCatalog.findById(id).orElseThrow().getStock();
        assertEquals(before - 1, after);

        assertEquals(50000, receipt.getPayAmount());
        assertEquals(0, receipt.getShippingFee());

    }

    @Test
    void checkout_수량이0이하면() {
        ProductCatalog productCatalog = new ProductCatalog();
        CheckOutService service = new CheckOutService(productCatalog, new SortedReentrantLockManager());
        Map<Long, Integer> req = new LinkedHashMap<>();
        req.put(1001L, 0);

        assertThrows(IllegalArgumentException.class, () -> service.checkOut(req));
    }

    @Test
    void checkOut_재고부족이면_예외_그리고_수량복귀() {
        ProductCatalog productCatalog = new ProductCatalog();
        CheckOutService service = new CheckOutService(productCatalog, new SortedReentrantLockManager());

        Map<Long, Integer> req = new LinkedHashMap<>();
        Long id = 1001L;

        int before = productCatalog.findById(id).orElseThrow().getStock();
        req.put(id, before + 1);

        assertThrows(SoldOutException.class, () -> service.checkOut(req));

        int after = productCatalog.findById(id).orElseThrow().getStock();
        assertEquals(before, after);
    }

}