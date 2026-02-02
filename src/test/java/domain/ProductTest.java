package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void canConsume_재고가_충분하면_true() {
        Product product = new Product(1000, 1l, "p", 10);

        assertTrue(product.canConsume(1));
        assertTrue(product.canConsume(10));
    }

    @Test
    void canConsume_재고가_부족하면() {
        Product product = new Product(1000, 1l, "p", 10);
        assertFalse(product.canConsume(20));
    }

    @Test
    void canConsume_재고가_0이하면() {
        Product product = new Product(1000, 1l, "p", 10);
        assertFalse(product.canConsume(-1));
        assertFalse(product.canConsume(0));
    }

    @Test
    void consume_호출() {
        Product product = new Product(1000, 1l, "p", 10);
        product.consume(5);
        assertEquals(5, product.getStock());
    }

}