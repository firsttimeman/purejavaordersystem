package service;

import concurrent.ProductLockManager;
import domain.OrderItem;
import domain.OrderReceipt;
import domain.Product;
import exception.SoldOutException;
import repo.ProductCatalog;

import java.util.*;
import java.util.concurrent.locks.Lock;

public class CheckOutService {
    private static final long SHIPPING_FEE = 2500;
    private static final long SHIPPING_LIMIT = 50000;

    private final ProductCatalog productCatalog;
    private final ProductLockManager productLockManager;

    public CheckOutService(ProductCatalog productCatalog, ProductLockManager productLockManager) {
        this.productCatalog = productCatalog;
        this.productLockManager = productLockManager;
    }

    public OrderReceipt checkOut(Map<Long, Integer> orderRequest) {

        for (Map.Entry<Long, Integer> entry : orderRequest.entrySet()) {
            if(entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("수량은 1 이상 이어야 합니다. 상품 ID = " + entry.getKey());
            }
        }

        List<Product> products = orderRequest.keySet().stream()
                .map(id -> productCatalog.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("상품이 없음: " + id)))
                .sorted(Comparator.comparingLong(Product::getId))
                .toList();

        List<Lock> lockList = productLockManager.lockAllByIds(orderRequest.keySet());

        try {
            for (Product product : products) {
                int amount = orderRequest.get(product.getId());
                if(!product.canConsume(amount)) {
                    throw new SoldOutException("재고 부족: " + product.getName());
                }
            }

            List<OrderItem> items = new ArrayList<>();
            long orderAmount = 0;
            for (Product product : products) {
                int amount = orderRequest.get(product.getId());
                product.consume(amount);
                OrderItem item = new OrderItem(product, amount);
                items.add(item);
                orderAmount += item.getTotalPrice();
            }

            long shippingFee = orderAmount < SHIPPING_LIMIT ? SHIPPING_FEE : 0;
            long pay = orderAmount + shippingFee;
            return new OrderReceipt(items, orderAmount, shippingFee, pay);

        } finally {
            productLockManager.unlockAllByIds(lockList);
        }

    }


}
