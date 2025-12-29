package com.carrot.app.domain.product.dto;

import com.carrot.app.domain.product.entity.Product.Status;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "카테고리를 하나 이상 선택해주세요")
    private Long categoryId;

    private String location;

    // 유지할 기존 이미지 ID 목록 (순서대로)
    @Size(max = 5, message = "이미지는 최대 5개까지만 유지할 수 있습니다.")
    private List<Long> keptImageIds;

    // 새로 업로드할 파일들
    @Size(max = 5, message = "이미지는 최대 5개까지만 업로드할 수 있습니다.")
    private List<MultipartFile> images;

    @NotNull(message = "판매 상태를 선택해주세요")
    private Status status;
}
