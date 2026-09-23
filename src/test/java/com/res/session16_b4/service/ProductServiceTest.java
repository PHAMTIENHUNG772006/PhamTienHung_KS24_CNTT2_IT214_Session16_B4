package com.res.session16_b4.service;

import com.res.session16_b4.config.CustomCacheErrorHandler;
import com.res.session16_b4.dto.ProductDTO;
import com.res.session16_b4.dto.UpdateProductRequest;
import com.res.session16_b4.exception.ResourceNotFoundException;
import com.res.session16_b4.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class ProductServiceTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("products");
        }
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CustomCacheErrorHandler customCacheErrorHandler;

    @BeforeEach
    void setUp() {
        productRepository.initData();
        productRepository.resetDbQueryCount();
        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("1. Kiểm thử @Cacheable: Lần 1 gọi DB, lần 2 lấy từ Cache (DB Query Count không tăng)")
    void testCacheable_GetProductById_HitsCacheOnSecondCall() {
        Long productId = 1L;

        // Lần gọi 1: Cache Miss -> Phải truy vấn DB
        long initialDbCount = productRepository.getDbQueryCount();
        ProductDTO firstCallResult = productService.getProductById(productId);

        assertThat(firstCallResult).isNotNull();
        assertThat(firstCallResult.getId()).isEqualTo(productId);
        assertThat(productRepository.getDbQueryCount()).isEqualTo(initialDbCount + 1);

        // Kiểm tra xem dữ liệu đã được nạp vào cache chưa
        Cache cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        assertThat(cache.get(productId)).isNotNull();

        // Lần gọi 2: Cache Hit -> Lấy từ Cache, DB Query Count KHÔNG TĂNG
        ProductDTO secondCallResult = productService.getProductById(productId);

        assertThat(secondCallResult).isNotNull();
        assertThat(secondCallResult.getName()).isEqualTo(firstCallResult.getName());
        assertThat(productRepository.getDbQueryCount()).isEqualTo(initialDbCount + 1); // Vẫn bằng 1
    }

    @Test
    @DisplayName("2. Kiểm thử CHIẾN LƯỢC @CacheEvict: Cập nhật DB và XÓA Cache cũ, lần đọc kế tiếp nạp từ DB")
    void testCacheEvict_UpdateProduct_ClearsCache() {
        Long productId = 2L;

        // Bước 1: Nạp vào cache trước
        productService.getProductById(productId);
        Cache cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        assertThat(cache.get(productId)).isNotNull();

        long dbQueriesBeforeUpdate = productRepository.getDbQueryCount();

        // Bước 2: Thực hiện Cập nhật bằng chiến lược @CacheEvict
        UpdateProductRequest updateReq = UpdateProductRequest.builder()
                .name("iPhone 16 Pro Max 512GB (Đã Nâng Cấp)")
                .description("Titanium Sa Mạc bản 512GB siêu mỏng")
                .price(new BigDecimal("39990000"))
                .stockQuantity(99)
                .category("Smartphone")
                .build();

        ProductDTO updatedDTO = productService.updateProduct(productId, updateReq);
        assertThat(updatedDTO.getName()).isEqualTo("iPhone 16 Pro Max 512GB (Đã Nâng Cấp)");

        // Bước 3: Xác minh Cache đã bị XÓA (Evicted)
        assertThat(cache.get(productId)).isNull();

        long dbQueriesAfterUpdate = productRepository.getDbQueryCount();

        // Bước 4: Lần đọc tiếp theo sau khi bị Evict -> Buộc phải gọi DB để tải dữ liệu mới
        ProductDTO subsequentRead = productService.getProductById(productId);
        assertThat(subsequentRead.getName()).isEqualTo("iPhone 16 Pro Max 512GB (Đã Nâng Cấp)");
        assertThat(subsequentRead.getPrice()).isEqualByComparingTo(new BigDecimal("39990000"));
        assertThat(productRepository.getDbQueryCount()).isEqualTo(dbQueriesAfterUpdate + 1);

        // Và dữ liệu mới đã được nạp lại vào Cache
        assertThat(cache.get(productId)).isNotNull();
    }

    @Test
    @DisplayName("3. Kiểm thử CHIẾN LƯỢC ĐỐI CHỨNG @CachePut: Ghi đè trực tiếp vào Cache, lần đọc kế tiếp không cần gọi DB")
    void testCachePut_UpdateProductWithPut_DirectlyOverwritesCache() {
        Long productId = 3L;

        // Bước 1: Nạp vào cache
        productService.getProductById(productId);
        Cache cache = Objects.requireNonNull(cacheManager.getCache("products"));

        // Bước 2: Cập nhật bằng @CachePut
        UpdateProductRequest updateReq = UpdateProductRequest.builder()
                .name("Bàn phím Keychron Q1 Pro RGB Special")
                .description("Switch Banana cao cấp")
                .price(new BigDecimal("5200000"))
                .stockQuantity(15)
                .category("Phụ kiện")
                .build();

        ProductDTO updated = productService.updateProductWithPut(productId, updateReq);
        assertThat(updated.getName()).isEqualTo("Bàn phím Keychron Q1 Pro RGB Special");

        // Bước 3: Kiểm tra Cache đã có giá trị mới NGAY LẬP TỨC (không bị null như Evict)
        Cache.ValueWrapper wrapper = cache.get(productId);
        assertThat(wrapper).isNotNull();
        ProductDTO cachedDto = (ProductDTO) wrapper.get();
        assertThat(cachedDto).isNotNull();
        assertThat(cachedDto.getName()).isEqualTo("Bàn phím Keychron Q1 Pro RGB Special");

        long dbQueriesAfterPut = productRepository.getDbQueryCount();

        // Bước 4: Lần đọc tiếp theo LẤY NGAY TỪ CACHE, DB không bị truy vấn thêm
        ProductDTO readAfterPut = productService.getProductById(productId);
        assertThat(readAfterPut.getName()).isEqualTo("Bàn phím Keychron Q1 Pro RGB Special");
        assertThat(productRepository.getDbQueryCount()).isEqualTo(dbQueriesAfterPut); // Không tăng thêm truy vấn DB nào!
    }

    @Test
    @DisplayName("4. Kiểm thử khi sản phẩm không tồn tại -> Bắn ngoại lệ ResourceNotFoundException")
    void testUpdateProduct_NotFound_ThrowsException() {
        Long nonExistentId = 99999L;
        UpdateProductRequest updateReq = UpdateProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("100000"))
                .stockQuantity(10)
                .category("Others")
                .build();

        assertThatThrownBy(() -> productService.getProductById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy sản phẩm");

        assertThatThrownBy(() -> productService.updateProduct(nonExistentId, updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy sản phẩm");
    }

    @Test
    @DisplayName("5. Kiểm thử CustomCacheErrorHandler (Fail-Open / Graceful Degradation)")
    void testCustomCacheErrorHandler_Resilience() {
        Cache cache = Objects.requireNonNull(cacheManager.getCache("products"));
        RuntimeException simulatedRedisTimeout = new RuntimeException("Redis connection timed out after 2000ms");

        // Kiểm tra xử lý lỗi khi GET cache bị lỗi: không throw exception ra ngoài
        assertDoesNotThrow(() -> customCacheErrorHandler.handleCacheGetError(simulatedRedisTimeout, cache, 1L));

        // Kiểm tra xử lý lỗi khi PUT cache bị lỗi: không throw exception
        assertDoesNotThrow(() -> customCacheErrorHandler.handleCachePutError(simulatedRedisTimeout, cache, 1L, "sampleValue"));

        // Kiểm tra xử lý lỗi khi EVICT cache bị lỗi: không throw exception làm chết transaction
        assertDoesNotThrow(() -> customCacheErrorHandler.handleCacheEvictError(simulatedRedisTimeout, cache, 1L));

        // Kiểm tra xử lý lỗi khi CLEAR cache bị lỗi
        assertDoesNotThrow(() -> customCacheErrorHandler.handleCacheClearError(simulatedRedisTimeout, cache));
    }
}
