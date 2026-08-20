package travelplanner.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import travelplanner.dto.storage.PressingUploadRequestDto;
import travelplanner.dto.storage.PressingUploadResponseDto;
import travelplanner.entity.User;
import travelplanner.exception.InvalidFileException;
import travelplanner.security.AuthenticationService;

@Service
@RequiredArgsConstructor
public class StorageService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp");
    private final AuthenticationService authenticationService;
    private final S3Presigner s3Presigner;

    @Value("${supabase.s3.bucket}")
    private String bucket;

    public PressingUploadResponseDto generateUploadResignedUrl(PressingUploadRequestDto request) {
        User user = authenticationService.getAuthenticatedUser();
        if (request.getFileSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds 5MB limit");
        }
        if (!ALLOWED_TYPES.contains(request.getContentType())) {
            throw new InvalidFileException(
                    "Invalid content type. Allowed: image/jpeg, image/png, image/webp");
        }

        String extension = getExtensionFromContentType(request.getContentType());

        String objectKey = String.format(
                "avatars/%s/%s.%s", user.getUserId(), UUID.randomUUID(), extension);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        return new PressingUploadResponseDto(uploadUrl, objectKey);
    }

    public String generateGetPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(2))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String getExtensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}
