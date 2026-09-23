package com.res.session16_b4.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.res.session16_b4.dto.ProductDTO;
import com.res.session16_b4.dto.UpdateProductRequest;
import com.res.session16_b4.exception.GlobalExceptionHandler;
import com.res.session16_b4.exception.ResourceNotFoundException;
import com.res.session16_b4.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products/1 - Thành công trả về 200 OK")
    void getProductById_Success() throws Exception {
        ProductDTO dto = ProductDTO.builder()
                .id(1L)
                .name("MacBook Pro")
                .description("M3 Max")
                .price(new BigDecimal("79990000"))
                .stockQuantity(50)
                .category("Laptop")
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.getProductById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro"));
    }

    @Test
    @DisplayName("GET /api/v1/products/999 - Không tìm thấy trả về 404 Not Found")
    void getProductById_NotFound() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: 999"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 999"));
    }

    @Test
    @DisplayName("PUT /api/v1/products/1 - Hợp lệ trả về 200 OK")
    void updateProduct_Success() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("MacBook Pro 16 inch M3 Max Updated")
                .description("Cập nhật mô tả")
                .price(new BigDecimal("82000000"))
                .stockQuantity(40)
                .category("Laptop")
                .build();

        ProductDTO updatedDto = ProductDTO.builder()
                .id(1L)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(1L), any(UpdateProductRequest.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro 16 inch M3 Max Updated"));
    }

    @Test
    @DisplayName("PUT /api/v1/products/1 - Validation thất bại khi thiếu trường trả về 400 Bad Request")
    void updateProduct_ValidationFailed() throws Exception {
        UpdateProductRequest invalidRequest = UpdateProductRequest.builder()
                .name("") // Blank name
                .price(new BigDecimal("-100")) // Negative price
                .stockQuantity(-5) // Negative stock
                .category("") // Blank category
                .build();

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.price").exists())
                .andExpect(jsonPath("$.data.stockQuantity").exists())
                .andExpect(jsonPath("$.data.category").exists());
    }
}
