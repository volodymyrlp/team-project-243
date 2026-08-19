package travelplanner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
