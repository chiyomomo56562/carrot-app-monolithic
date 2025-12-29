package com.carrot.app.domain.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.carrot.app.domain.product.dto.ProductResponse;
import com.carrot.app.domain.product.service.ProductService;
import com.carrot.app.global.exception.UnauthorizedException;

import java.time.LocalDateTime;

import com.carrot.app.global.security.CustomUserDetails;
import com.carrot.app.domain.user.entity.User;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ProductService productService;

        @Test
        @DisplayName("상품 등록 성공")
        void createProduct_Success() throws Exception {
                // given
                ProductResponse response = ProductResponse.builder()
                                .id(1L)
                                .title("Title")
                                .status("ON_SALE")
                                .createdAt(LocalDateTime.now())
                                .build();
                given(productService.createProduct(any(), any())).willReturn(response);

                MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg",
                                "content".getBytes());

                User mockUser = User.builder().id(1L).email("test@example.com").password("pw").role(User.Role.ROLE_USER)
                                .status(User.Status.ACTIVE).build();
                CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

                // when & then
                mockMvc.perform(multipart("/api/products/new")
                                .file(image)
                                .param("title", "Title")
                                .param("categoryId", "1")
                                .param("description", "Desc")
                                .param("price", "1000")
                                .param("location", "Seoul")
                                .with(csrf())
                                .with(user(customUserDetails))
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("상품 등록 실패 - 이미지 개수 초과 (@Size Validation)")
        void createProduct_Fail_TooManyImages() throws Exception {
                // given
                MockMultipartFile image = new MockMultipartFile("images", "test.jpg", "image/jpeg",
                                "content".getBytes());
                // Max 5 images allowed

                User mockUser = User.builder().id(1L).email("test@example.com").password("pw").role(User.Role.ROLE_USER)
                                .status(User.Status.ACTIVE).build();
                CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

                // when & then
                mockMvc.perform(multipart("/api/products/new")
                                .file(image).file(image).file(image).file(image).file(image).file(image) // 6 images
                                .param("title", "Title")
                                .param("description", "Desc")
                                .param("price", "1000")
                                .param("categoryId", "1")
                                .param("location", "Seoul")
                                .with(csrf())
                                .with(user(customUserDetails)))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
                // Validation exception handled by GlobalExceptionHandler
        }

        @Test
        @DisplayName("상품 수정 성공")
        void updateProduct_Success() throws Exception {
                // given
                ProductResponse response = ProductResponse.builder().id(1L).title("Updated").status("ON_SALE")
                                .createdAt(LocalDateTime.now()).build();
                given(productService.updateProduct(eq(1L), any(), anyString())).willReturn(response);

                // Put/Patch with Multipart is tricky in MockMvc.
                // MockMvcRequestBuilder 'multipart' defaults to POST. We must override method.

                MockMultipartFile image = new MockMultipartFile("images", "new.jpg", "image/jpeg",
                                "content".getBytes());

                User mockUser = User.builder().id(1L).email("test@example.com").password("pw").role(User.Role.ROLE_USER)
                                .status(User.Status.ACTIVE).build();
                CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

                // when & then
                mockMvc.perform(multipart(HttpMethod.PATCH, "/api/products/1/edit")
                                .file(image)
                                .param("title", "Updated")
                                .param("description", "Desc")
                                .param("price", "2000")
                                .param("categoryId", "1")
                                .param("status", "ON_SALE") // Enum conversion check
                                .with(csrf())
                                .with(user(customUserDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Updated"));
        }

        @Test
        @DisplayName("상품 수정 실패 - 권한 없음")
        void updateProduct_Fail_Unauthorized() throws Exception {
                // given
                given(productService.updateProduct(eq(1L), any(), eq("attacker")))
                                .willThrow(new UnauthorizedException("Unauthorized"));

                User mockUser = User.builder().id(2L).email("attacker").password("pw").role(User.Role.ROLE_USER)
                                .status(User.Status.ACTIVE).build();
                CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);

                // when & then
                mockMvc.perform(multipart(HttpMethod.PATCH, "/api/products/1/edit")
                                .param("title", "Updated")
                                .param("description", "Desc")
                                .param("price", "2000")
                                .param("categoryId", "1")
                                .param("status", "ON_SALE")
                                .with(csrf())
                                .with(user(customUserDetails)))
                                .andDo(print())
                                .andExpect(status().isForbidden()); // UnauthorizedException -> 403 or 401 depending on
                                                                    // Handler
        }
}
