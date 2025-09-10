import domain.OrderReceipt;
import domain.Product;
import exception.SoldOutException;
import repo.ProductCatalog;
import service.CheckOutService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainApp {

    private static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


    public static void main(String[] args) throws IOException {
        ProductCatalog catalog = new ProductCatalog();
        CheckOutService checkOutService = new CheckOutService(catalog);

        while(true) {
            String cmd = printPrompt("입력(o[order]: 주문, q[quit]: 종료) : ");
            if(cmd == null) break;
            cmd = cmd.trim().toLowerCase();

            switch (cmd) {
                case "q", "quit" -> {
                    System.out.println("고객님의 주문 감사합니다.");
                    return;
                }
                case "o", "order" -> {
                    printCatalog(catalog.findAll());
                    handleOrder(checkOutService);
                }
            }

        }

    }

    private static void handleOrder(CheckOutService checkOutService) throws IOException {
        Map<Long, Integer> req = new LinkedHashMap<>();

        while (true) {
            String idStr = printPrompt("상품번호 : ");
            if (isBlank(idStr)) break;
            String str = printPrompt("수량 : ");
            if (isBlank(str)) break;

            try {
                long id = Long.parseLong(idStr.trim());
                int amount = Integer.parseInt(str.trim());
                if (amount <= 0) {
                    System.out.println("수량은 1 이상이어야 합니다.");
                    continue;
                }
                req.merge(id, amount, Integer::sum);
            } catch (NumberFormatException e) {
                System.out.println("잘못된 숫자 입력입니다. 다시 입력해주세요");
            }
        }

        if (req.isEmpty()) return;

        try {
            OrderReceipt receipt = checkOutService.checkOut(req);
            System.out.println();
            System.out.println(receipt);
        } catch (SoldOutException e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void printCatalog(Collection<Product> products) {
        System.out.println();
        System.out.println();
        System.out.printf("%-8s  %-48s  %8s  %6s%n", "상품번호", "상품명", "판매가격", "재고수");
        for (Product p : products) {
            long price = p.getPrice();
            System.out.printf("%-8d  %-48s  %8s  %6d%n",
                    p.getId(), p.getName(), price, p.getStock());
        }
        System.out.println();

    }

    private static String printPrompt(String prompt) throws IOException {
        System.out.print(prompt);
        return br.readLine();
    }
}
