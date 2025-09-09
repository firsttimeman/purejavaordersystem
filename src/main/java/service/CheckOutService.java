package service;

import domain.OrderItem;
import domain.OrderReceipt;
import domain.Product;
import exception.SoldOutException;
import repo.ProductCatalog;

import java.util.*;

public class CheckOutService {
    private static final long SHIPPING_FEE = 2500;
    private static final long SHIPPING_LIMIT = 50000;

    private final ProductCatalog productCatalog;

    public CheckOutService(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    public OrderReceipt checkOut(Map<Long, Integer> orderRequest) {

        List<Product> products = orderRequest.keySet().stream()
                .map(id -> productCatalog.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("상품이 없음: " + id)))
                .sorted(Comparator.comparingLong(Product::getId))
                .toList();

        lockAll(products);

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
            unlockAll(products);
        }


    }

    private void unlockAll(List<Product> products) {
        ListIterator<Product> it = products.listIterator(products.size());
        while (it.hasPrevious()) {
            it.previous().unlock();
        }
    }

    private void lockAll(List<Product> products) {
        for (Product product : products) {
            product.lock();
        }
    }
}
