package com.res.session16_b4.controller;

import com.res.session16_b4.dto.ApiResponse;
import com.res.session16_b4.dto.ProductDTO;
import com.res.session16_b4.dto.UpdateProductRequest;
import com.res.session16_b4.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Lấy thông tin chi tiết sản phẩm.
     * Sử dụng @Cacheable: Lần đầu truy vấn DB & ghi Cache, các lần sau đọc trực tiếp từ Redis Cache.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id) {
        log.info("API GET /api/v1/products/{}", id);
        ProductDTO productDTO = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(productDTO, "Lấy thông tin sản phẩm thành công"));
    }

    /**
     * Cập nhật thông tin sản phẩm - CHIẾN LƯỢC CHÍNH ĐƯỢC CHỌN: @CacheEvict (Cache Invalidation)
     * Thích hợp tối ưu cho hệ thống E-commerce có tỷ lệ Đọc:Ghi = 100:1.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("API PUT /api/v1/products/{} [Strategy: @CacheEvict]", id);
        ProductDTO updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật sản phẩm thành công (Cache cũ đã bị xóa)"));
    }

    /**
     * Cập nhật thông tin sản phẩm - CHIẾN LƯỢC ĐỐI CHỨNG: @CachePut (Cache Overwrite)
     * Dùng để thử nghiệm so sánh cơ chế ghi đè trực tiếp vào cache.
     */
    @PutMapping("/{id}/put-demo")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProductWithPut(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("API PUT /api/v1/products/{}/put-demo [Strategy: @CachePut]", id);
        ProductDTO updated = productService.updateProductWithPut(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật sản phẩm thành công (Cache đã được ghi đè trực tiếp)"));
    }
}
