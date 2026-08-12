package com.bp20.backend.global.storage;

import com.bp20.backend.global.config.StorageProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * product-image-storage.s3.bucket이 설정되어 있지 않을 때 사용하는 기본 저장소.
 * ImageStorageConfig가 등록해두는 정적 리소스 핸들러(/product-images/**)를 통해 그대로 서빙된다.
 */
public class LocalDiskImageStorage implements ImageStorage {

    private final Path baseDir;
    private final String baseUrl;

    public LocalDiskImageStorage(StorageProperties properties) {
        this.baseDir = Path.of(properties.local().dir());
        this.baseUrl = properties.local().baseUrl();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장 디렉토리를 만들 수 없습니다: " + baseDir, e);
        }
    }

    @Override
    public String store(byte[] data, String filename) {
        try {
            Files.write(baseDir.resolve(filename), data);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장에 실패했습니다: " + filename, e);
        }
        return "/api/public/product-images/" + filename;
    }

    @Override
    public StoredImage load(String filename) {
        Path imagePath = baseDir.resolve(filename).normalize();
        if (!imagePath.startsWith(baseDir.normalize())) {
            throw new IllegalArgumentException("올바르지 않은 이미지 경로입니다.");
        }

        try {
            String contentType = Files.probeContentType(imagePath);
            return new StoredImage(
                    Files.readAllBytes(imagePath),
                    contentType == null ? "image/png" : contentType
            );
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 불러올 수 없습니다: " + filename, e);
        }
    }
}
