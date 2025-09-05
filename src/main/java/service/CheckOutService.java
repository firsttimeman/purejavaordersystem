package service;

import domain.OrderItem;
import domain.OrderReceipt;
import domain.Product;
import exception.SoldOutException;
import repo.ProductCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckOutService {
    private static final long SHIPPING_FEE = 2500;
    private static final long SHIPPING_LIMIT = 50000;

    private final ProductCatalog productCatalog;

    public CheckOutService(ProductCatalog productCatalog) {
        this.productCatalog = productCatalog;
    }

    public OrderReceipt checkOut(Map<Long, Integer> orderRequest) {
        List<Product> products = new ArrayList<>();
        for (Long id : orderRequest.keySet()) {
            Product product = productCatalog.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("상품 없음: " + id));
            products.add(product);
        }

        for (Product product : products) {
            int amount = orderRequest.get(product.getId());
            if(!product.hasStock(amount)) {
                throw new SoldOutException("SoldOutException 품절현상 발생");
            }
        }

        List<OrderItem> items = new ArrayList<>();
        long orderAmount = 0;

        for (Product product : products) {
            Integer amount = orderRequest.get(product.getId());
            product.consume(amount);
            OrderItem orderItem = new OrderItem(product, amount);
            items.add(orderItem);
            orderAmount += orderItem.getTotalPrice();
        }

        long shipping = orderAmount < SHIPPING_LIMIT ? SHIPPING_FEE : 0;
        long pay = orderAmount + shipping;
        return new OrderReceipt(items, orderAmount, shipping, pay);

    }
}
