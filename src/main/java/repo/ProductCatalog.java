package repo;

import domain.Product;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProductCatalog {

    private Map<Long, Product> products = new HashMap<>();

    public ProductCatalog() {
        loadCatalog();
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Collection<Product> findAll() {
        return products.values();
    }


    private void loadCatalog() {

        String[][] rows = new String[][]{
                {"1001","무선 블루투스 이어폰","79000","30"},
                {"1002","스마트 워치","159000","15"},
                {"1003","게이밍 키보드","89000","25"},
                {"1004","휴대용 보조 배터리 20000mAh","39000","50"},
                {"1005","에코 텀블러 500ml","12000","100"},
                {"1006","오가닉 코튼 티셔츠","25000","80"},
                {"1007","프리미엄 원두커피 세트","45000","20"},
                {"1008","LED 스탠드","34000","40"},
                {"1009","노트북 파우치 15인치","29000","35"},
                {"1010","헤어 드라이기","69000","18"}
        };


        for (String[] row : rows) {
            long id = Long.parseLong(row[0]);
            String name = row[1];
            long price = Long.parseLong(row[2]);
            int stock = Integer.parseInt(row[3]);
            products.put(id, new Product(price, id, name, stock));
        }

    }
}
