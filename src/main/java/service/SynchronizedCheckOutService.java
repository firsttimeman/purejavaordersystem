package service;

import domain.OrderItem;
import domain.OrderReceipt;
import domain.Product;
import exception.SoldOutException;
import repo.ProductCatalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SynchronizedCheckOutService {

    private static final long SHIPPING_FEE = 2500;
    private static final long SHIPPING_LIMIT = 50000;

    private ProductCatalog productCatalog;

    public SynchronizedCheckOutService(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    public synchronized OrderReceipt checkOut(Map<Long, Integer> orderRequest) {

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

    }
}
