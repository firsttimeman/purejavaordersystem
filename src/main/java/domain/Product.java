package domain;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private final long id;
    private final String name;
    private final long price;
    private int stock;



    public Product(long price, long id, String name, int stock) {
        this.price = price;
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }




    public int getStock() {
        return stock;
    }

    public boolean canConsume(int n ) {
        return n > 0 && stock >= n;
    }

    public void consume(int n) {
        stock -= n;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
