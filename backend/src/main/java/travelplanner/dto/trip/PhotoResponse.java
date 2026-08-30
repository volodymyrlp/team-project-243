package travelplanner.dto.trip;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID photoId,
        String url,
        UUID uploaderId,
        LocalDateTime createdAt
) {}
