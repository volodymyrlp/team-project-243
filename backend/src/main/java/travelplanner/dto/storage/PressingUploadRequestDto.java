package travelplanner.dto.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PressingUploadRequestDto {
    @NotBlank(message = "Content type is required")
    private String contentType;
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;
}
