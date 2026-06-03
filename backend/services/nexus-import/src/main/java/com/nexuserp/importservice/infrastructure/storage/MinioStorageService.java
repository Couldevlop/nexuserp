package com.nexuserp.importservice.infrastructure.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.BucketExistsArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final String errorReportBucket;
    private final String templatesBucket;

    public MinioStorageService(
            @Value("${minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey,
            @Value("${nexuserp.import.error-reports.bucket:nexuserp-import-errors}") String errorReportBucket,
            @Value("${nexuserp.import.templates.bucket:nexuserp-import-templates}") String templatesBucket) {
        this.minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
        this.errorReportBucket = errorReportBucket;
        this.templatesBucket = templatesBucket;
    }

    @PostConstruct
    public void ensureBucketsExist() {
        ensureBucket(errorReportBucket);
        ensureBucket(templatesBucket);
    }

    private void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create bucket {}: {}", bucket, e.getMessage());
        }
    }

    public String storeErrorReport(String tenantId, String filename, byte[] content) {
        String objectName = tenantId + "/" + filename;
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(errorReportBucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build()
            );
            return getPresignedUrl(errorReportBucket, objectName, 7);
        } catch (Exception e) {
            log.error("Failed to store error report in MinIO: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getPresignedUrl(String bucket, String objectName, int expiryDays) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(expiryDays, TimeUnit.DAYS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            return null;
        }
    }
}
