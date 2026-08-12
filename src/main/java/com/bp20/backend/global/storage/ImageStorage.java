package com.bp20.backend.global.storage;

/**
 * 이미지 바이트를 저장하고 외부에서 접근 가능한 URL을 돌려준다.
 * 구현체는 {@link com.bp20.backend.global.config.StorageProperties}의 s3.bucket 설정 여부에 따라
 * 로컬 디스크(LocalDiskImageStorage) 또는 S3(S3ImageStorage) 중 하나가 선택된다.
 */
public interface ImageStorage {

    String store(byte[] data, String filename);

    StoredImage load(String filename);

    record StoredImage(byte[] data, String contentType) {
    }
}
