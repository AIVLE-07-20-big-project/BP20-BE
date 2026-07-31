package com.bp20.backend.global.storage;

import com.bp20.backend.global.config.StorageProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * storage.s3.bucket이 설정되어 있을 때 사용하는 저장소.
 *
 * accessKey/secretKey를 명시적으로 받는 이유: 이 프로젝트는 .env 파일을
 * spring.config.import(application-local.yml)로만 불러오는데, 이건 Spring의
 * Environment에만 값을 넣어줄 뿐 실제 OS 환경변수로는 만들어주지 않는다.
 * AWS SDK의 기본 자격 증명 체인(EnvironmentVariableCredentialsProvider 등)은
 * System.getenv()로 진짜 OS 환경변수만 읽기 때문에, .env에 AWS_ACCESS_KEY_ID를
 * 넣어놔도 SDK 기본 체인으로는 찾지 못해 "Unable to load credentials" 에러가 난다.
 * 그래서 Spring이 읽은 값을 여기로 직접 전달받아 StaticCredentialsProvider로 넘긴다.
 */
public class S3ImageStorage implements ImageStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ImageStorage(StorageProperties properties, String accessKey, String secretKey) {
        this.bucket = properties.s3().bucket();

        AwsCredentialsProvider credentialsProvider =
                (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank())
                        ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                        : DefaultCredentialsProvider.create();

        this.s3Client = S3Client.builder()
                .region(Region.of(properties.s3().region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Override
    public String store(byte[] data, String filename) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(filename)
                        .contentType("image/png")
                        .build(),
                RequestBody.fromBytes(data)
        );
        return s3Client.utilities().getUrl(b -> b.bucket(bucket).key(filename)).toString();
    }
}
