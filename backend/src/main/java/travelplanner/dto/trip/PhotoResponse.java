package travelplanner.dto.trip;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Response payload representing a trip photo")
public record PhotoResponse(
        @Schema(
                description = "Unique identifier of the photo",
                example = "d3b07384-d113-4944-9cbe-123456789abc"
        )
        UUID photoId,

        @Schema(
                description = "URL path to access the photo",
                example = "/uploads/photo.jpg"
        )
        String url,

        @Schema(
                description = "ID of the user who uploaded the photo",
                example = "e4c18495-e224-4a55-adcf-234567890def"
        )
        UUID uploaderId,

        @Schema(
                description = "Timestamp when the photo was uploaded",
                example = "2026-07-01T12:00:00"
        )
        LocalDateTime createdAt
) {}
