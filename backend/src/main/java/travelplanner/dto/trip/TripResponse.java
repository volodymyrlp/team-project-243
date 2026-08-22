package travelplanner.dto.trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TripResponse(
        UUID tripId,
        String title,
        String description,
        BigDecimal budget,
        String currency,
        String coverUrl,
        Boolean isPublic,
        LocalDateTime createdAt
) {}
