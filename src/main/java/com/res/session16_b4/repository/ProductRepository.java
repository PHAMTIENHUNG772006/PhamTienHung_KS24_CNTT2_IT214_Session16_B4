package com.res.session16_b4.repository;

import com.res.session16_b4.model.Product;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Repository
public class ProductRepository {

    private final Map<Long, Product> database = new ConcurrentHashMap<>();
    private final AtomicLong dbQueryCount = new AtomicLong(0);

    @PostConstruct
    public void initData() {
        Product p1 = Product.builder()
                .id(1L)
                .name("MacBook Pro 16 inch M3 Max")
                .description("Apple Silicon M3 Max, 36GB RAM, 1TB SSD")
                .price(new BigDecimal("79990000"))
                .stockQuantity(50)
                .category("Laptop")
                .updatedAt(LocalDateTime.now())
                .build();

        Product p2 = Product.builder()
                .id(2L)
                .name("iPhone 16 Pro Max 256GB")
                .description("Titanium Sa Mạc, Chip A18 Pro")
                .price(new BigDecimal("34990000"))
                .stockQuantity(120)
                .category("Smartphone")
                .updatedAt(LocalDateTime.now())
                .build();

        Product p3 = Product.builder()
                .id(3L)
                .name("Bàn phím cơ Keychron Q1 Pro")
                .description("Không dây, Hot-swap, QMK/VIA")
                .price(new BigDecimal("4590000"))
                .stockQuantity(35)
                .category("Phụ kiện")
                .updatedAt(LocalDateTime.now())
                .build();

        database.put(p1.getId(), p1);
        database.put(p2.getId(), p2);
        database.put(p3.getId(), p3);

        log.info("Initialized mock database with {} products", database.size());
    }

    public Optional<Product> findById(Long id) {
        long currentCount = dbQueryCount.incrementAndGet();
        log.warn(">>> [DATABASE QUERY] Accessing Database to find Product ID: {} (Total DB queries: {})", id, currentCount);
        Product product = database.get(id);
        if (product == null) {
            return Optional.empty();
        }
        // Return a copy to avoid in-memory reference mutation
        return Optional.of(cloneProduct(product));
    }

    public Product save(Product product) {
        log.info(">>> [DATABASE WRITE] Saving Product to Database: ID={}", product.getId());
        product.setUpdatedAt(LocalDateTime.now());
        database.put(product.getId(), cloneProduct(product));
        return cloneProduct(product);
    }

    public boolean existsById(Long id) {
        return database.containsKey(id);
    }

    public long getDbQueryCount() {
        return dbQueryCount.get();
    }

    public void resetDbQueryCount() {
        dbQueryCount.set(0);
    }

    private Product cloneProduct(Product original) {
        return Product.builder()
                .id(original.getId())
                .name(original.getName())
                .description(original.getDescription())
                .price(original.getPrice())
                .stockQuantity(original.getStockQuantity())
                .category(original.getCategory())
                .updatedAt(original.getUpdatedAt())
                .build();
    }
}
