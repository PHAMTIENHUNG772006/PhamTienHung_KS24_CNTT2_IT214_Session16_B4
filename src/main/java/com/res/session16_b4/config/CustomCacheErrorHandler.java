package com.res.session16_b4.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

/**
 * CustomCacheErrorHandler giúp hệ thống xử lý lỗi khi thao tác với Redis thất bại (Redis down, network timeout).
 * Triển khai mẫu hình "Fail-Open" / "Graceful Degradation":
 * - Khi Cache GET lỗi -> log warning và trả về null để Spring Cache tự động fallback gọi trực tiếp vào Database.
 * - Khi Cache PUT lỗi -> log warning, không làm gián đoạn transaction của người dùng.
 * - Khi Cache EVICT lỗi -> log error/critical, kích hoạt cơ chế fallback cảnh báo (có thể đẩy vào Dead Letter Queue/Retry).
 */
@Slf4j
@Component
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.error(">>> [CACHE GET ERROR] Không thể đọc key [{}] từ cache [{}]. Lỗi: {}. Fallback trực tiếp về DB.",
                key, cache.getName(), exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.error(">>> [CACHE PUT ERROR] Không thể ghi đè key [{}] vào cache [{}]. Lỗi: {}. Bỏ qua để tránh fail request.",
                key, cache.getName(), exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.error(">>> [CACHE EVICT ERROR - CRITICAL] Xóa key [{}] từ cache [{}] THẤT BẠI! Lỗi: {}. " +
                "Dữ liệu có nguy cơ bị stale nếu TTL chưa hết. Cần gửi alert hoặc đẩy vào Retry Queue!",
                key, cache.getName(), exception.getMessage());
        // Trong môi trường Production thực tế:
        // CacheEvictRetryService.enqueueEvictionTask(cache.getName(), key);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.error(">>> [CACHE CLEAR ERROR] Không thể xóa toàn bộ cache [{}]. Lỗi: {}",
                cache.getName(), exception.getMessage());
    }
}
