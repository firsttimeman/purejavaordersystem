package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void getTotalPrice() {
        Product product = new Product(1000, 1L, "이어폰", 10);
        OrderItem item = new OrderItem(product, 5);

        assertEquals(5000, item.getTotalPrice());
    }

}