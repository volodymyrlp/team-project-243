package travelplanner.dto.trip;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Response payload representing a trip")
public record TripResponse(
        @Schema(
                description = "Unique identifier of the trip",
                example = "d3b07384-d113-4944-9cbe-123456789abc"
        )
        UUID tripId,

        @Schema(
                description = "Title of the trip",
                example = "Summer in Italy"
        )
        String title,

        @Schema(
                description = "Destination city or country",
                example = "Rome, Italy"
        )
        String destination,

        @Schema(
                description = "Start date of the trip",
                example = "2026-07-01"
        )
        LocalDate startDate,

        @Schema(
                description = "End date of the trip",
                example = "2026-07-10"
        )
        LocalDate endDate,

        @Schema(
                description = "Detailed trip description",
                example = "Exploring historic landmarks and local food."
        )
        String description,

        @Schema(
                description = "Total estimated budget",
                example = "1200.00"
        )
        BigDecimal budget,

        @Schema(
                description = "ISO 4217 currency code",
                example = "EUR"
        )
        String currency,

        @Schema(
                description = "URL for the trip cover image",
                example = "/uploads/cover.jpg"
        )
        String coverUrl,

        @Schema(
                description = "Whether the trip is publicly visible in catalog",
                example = "true"
        )
        Boolean isPublic,

        @Schema(
                description = "Timestamp when the trip was created",
                example = "2026-07-01T12:00:00"
        )
        LocalDateTime createdAt
) {}
