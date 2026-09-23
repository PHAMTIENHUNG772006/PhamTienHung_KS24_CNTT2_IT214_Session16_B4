package com.res.session16_b4.service;

import com.res.session16_b4.dto.ProductDTO;
import com.res.session16_b4.dto.UpdateProductRequest;
import com.res.session16_b4.exception.ResourceNotFoundException;
import com.res.session16_b4.model.Product;
import com.res.session16_b4.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Cacheable(value = "products", key = "#productId")
    public ProductDTO getProductById(Long productId) {
        log.info(">>> [SERVICE] getProductById() được gọi với ID: {} (Chưa có trong Cache, phải nạp từ DB)", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));
        return ProductDTO.fromEntity(product);
    }

    /**
     * CHIẾN LƯỢC 1 (ĐƯỢC CHỌN CHO HỆ THỐNG ĐỌC/GHI 100:1): @CacheEvict
     * - Cập nhật dữ liệu vào DB trước.
     * - Xóa bỏ key tương ứng trong Cache để tránh rủi ro Race Condition khi ghi song song và tránh lãng phí RAM.
     * - Lần đọc tiếp theo sẽ tự động nạp bản ghi mới nhất từ DB vào Cache theo cơ chế Lazy Loading.
     */
    @Override
    @CacheEvict(value = "products", key = "#productId")
    public ProductDTO updateProduct(Long productId, UpdateProductRequest request) {
        log.info(">>> [SERVICE - CACHE_EVICT] updateProduct() bắt đầu xử lý cho Product ID: {}", productId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm để cập nhật với ID: " + productId));

        // Cập nhật thông tin thực thể
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStockQuantity(request.getStockQuantity());
        existingProduct.setCategory(request.getCategory());

        // Lưu vào Database
        Product updatedProduct = productRepository.save(existingProduct);
        log.info(">>> [SERVICE - CACHE_EVICT] Đã cập nhật DB cho Product ID: {}. Cache key 'products::{}' sẽ bị XÓA!",
                productId, productId);

        return ProductDTO.fromEntity(updatedProduct);
    }

    /**
     * CHIẾN LƯỢC 2 (ĐỐI CHỨNG): @CachePut
     * - Cập nhật dữ liệu vào DB.
     * - Ghi đè ngay lập tức dữ liệu mới vào Cache key 'products::{productId}'.
     */
    @Override
    @CachePut(value = "products", key = "#productId")
    public ProductDTO updateProductWithPut(Long productId, UpdateProductRequest request) {
        log.info(">>> [SERVICE - CACHE_PUT] updateProductWithPut() bắt đầu xử lý cho Product ID: {}", productId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm để cập nhật với ID: " + productId));

        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStockQuantity(request.getStockQuantity());
        existingProduct.setCategory(request.getCategory());

        Product updatedProduct = productRepository.save(existingProduct);
        log.info(">>> [SERVICE - CACHE_PUT] Đã cập nhật DB cho Product ID: {}. Cache key 'products::{}' được GHI ĐÈ TRỰC TIẾP!",
                productId, productId);

        return ProductDTO.fromEntity(updatedProduct);
    }
}
