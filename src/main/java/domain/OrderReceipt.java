package domain;

import java.util.List;

public class OrderReceipt {

    private final List<OrderItem> items;
    private final long orderAmount;
    private final long shippingFee;
    private final long payAmount;

    public OrderReceipt(List<OrderItem> items, long orderAmount, long shippingFee, long payAmount) {
        this.items = items;
        this.orderAmount = orderAmount;
        this.shippingFee = shippingFee;
        this.payAmount = payAmount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public long getOrderAmount() {
        return orderAmount;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public long getPayAmount() {
        return payAmount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("주문 내역: \n");
        sb.append("-----------------------------------------------\n");
        sb.append("\n");
        for (OrderItem item : items) {
            sb.append(item.getProduct().getName())
                    .append(" - ")
                    .append(item.getQuantity())
                    .append("개\n");
        }
        sb.append("-----------------------------------------------\n");
        sb.append("주문금액: ").append(orderAmount).append("\n");
        sb.append("-----------------------------------------------\n");
        if (shippingFee > 0) {
            sb.append("배송비: ").append(shippingFee).append("\n");
        }
        sb.append("지불금액: ").append(payAmount).append("\n");
        sb.append("-----------------------------------------------\n");

        return sb.toString();

    }
}
