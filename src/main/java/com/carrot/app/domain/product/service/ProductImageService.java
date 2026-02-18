package com.carrot.app.domain.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.app.domain.product.entity.ProductImage;
import com.carrot.app.domain.product.repository.ProductImageRepository;
import com.carrot.app.infra.storage.StorageProviderFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final StorageProviderFactory storageProviderFactory;
    private final ProductImageRepository productImageRepository;

    // 이미지 업로드
    public String uploadImage(MultipartFile image) {
        return storageProviderFactory.getProvider().upload(image);
    }

    // 여러 이미지 업로드
    public List<String> uploadImages(List<MultipartFile> images) {
        return images.stream()
                .filter(file -> !file.isEmpty())
                .map(this::uploadImage)
                .collect(java.util.stream.Collectors.toList());
    }

    // S3에서 이미지 삭제
    public void deleteImageFromS3(String imageUrl) {
        storageProviderFactory.getProvider().delete(imageUrl);
    }

    // 여러 이미지를 S3에서 삭제
    public void deleteImagesFromS3(List<String> imageUrls) {
        imageUrls.forEach(this::deleteImageFromS3);
    }

    // 이미지를 db에 저장
    public void saveProductImage(ProductImage productImage) {
        productImageRepository.save(productImage);
    }
}
