package travelplanner.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travelplanner.dto.storage.PressingUploadRequestDto;
import travelplanner.dto.storage.PressingUploadResponseDto;
import travelplanner.service.StorageService;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping("/presign")
    public ResponseEntity<PressingUploadResponseDto> presign(
            @RequestBody @Valid PressingUploadRequestDto requestDto) {
        return ResponseEntity.ok(storageService.generateUploadResignedUrl(requestDto));
    }

    // Новий ендпоінт для обкладинки трипу
    @PostMapping("/trips/{tripId}/cover/presign")
    public ResponseEntity<PressingUploadResponseDto> presignTripCover(
            @PathVariable UUID tripId,
            @RequestBody @Valid PressingUploadRequestDto requestDto) {
        return ResponseEntity.ok(storageService.generateTripCoverUploadUrl(tripId, requestDto));
    }

    // Новий ендпоінт для фото трипу
    @PostMapping("/trips/{tripId}/photos/presign")
    public ResponseEntity<PressingUploadResponseDto> presignTripPhoto(
            @PathVariable UUID tripId,
            @RequestBody @Valid PressingUploadRequestDto requestDto) {
        return ResponseEntity.ok(storageService.generateTripPhotoUploadUrl(tripId, requestDto));
    }
}
