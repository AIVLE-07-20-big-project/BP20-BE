package com.bp20.backend.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ObjectStorageService {
    private final S3Client s3Client;

    @Value("${storage.s3.bucket:}")
    private String bucket;

    @Value("${storage.s3.upload-prefix:uploads/v1}")
    private String uploadPrefix;

    public boolean enabled() {
        return bucket != null && !bucket.isBlank();
    }

    public StoredObject uploadCsv(MultipartFile file, String jobId) {
        if (!enabled()) {
            return null;
        }
        String safePrefix = uploadPrefix.replaceAll("^/+|/+$", "");
        String key = safePrefix + "/" + jobId + ".csv";
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("text/csv")
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredObject(bucket, key);
        } catch (IOException e) {
            throw new IllegalStateException("CSV 파일을 읽을 수 없습니다.", e);
        }
    }

    public void delete(StoredObject object) {
        if (object == null) {
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(object.bucket())
                .key(object.key())
                .build());
    }

    public record StoredObject(String bucket, String key) {}
}
