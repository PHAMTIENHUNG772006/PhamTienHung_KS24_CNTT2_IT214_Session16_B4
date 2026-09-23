package com.res.session16_b4.service;

import com.res.session16_b4.dto.ProductDTO;
import com.res.session16_b4.dto.UpdateProductRequest;

public interface ProductService {

    /**
     * Lấy thông tin chi tiết sản phẩm.
     * Áp dụng @Cacheable: nếu có trong Cache thì trả về ngay, nếu không thì truy vấn DB và lưu Cache.
     */
    ProductDTO getProductById(Long productId);

    /**
     * Cập nhật thông tin sản phẩm - Chiến lược CHÍNH: @CacheEvict (Cache Invalidation).
     * Cập nhật vào DB và xóa cache cũ tương ứng, để lượt đọc sau tự nạp dữ liệu mới.
     */
    ProductDTO updateProduct(Long productId, UpdateProductRequest request);

    /**
     * Cập nhật thông tin sản phẩm - Chiến lược THỬ NGHIỆM ĐỐI CHỨNG: @CachePut (Cache Overwrite).
     * Cập nhật vào DB và ghi đè trực tiếp giá trị mới trả về vào Cache.
     */
    ProductDTO updateProductWithPut(Long productId, UpdateProductRequest request);
}
