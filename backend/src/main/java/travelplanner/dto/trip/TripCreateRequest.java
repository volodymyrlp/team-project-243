package travelplanner.dto.trip;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request payload for creating a new trip")
public record TripCreateRequest(
        @Schema(
                description = "Title of the trip",
                example = "Summer in Italy"
        )
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Schema(
                description = "Destination city or country",
                example = "Rome, Italy"
        )
        @Size(max = 255, message = "Destination must not exceed 255 characters")
        String destination,

        @Schema(
                description = "Start date of the trip (YYYY-MM-DD)",
                example = "2026-07-01"
        )
        LocalDate startDate,

        @Schema(
                description = "End date of the trip (YYYY-MM-DD)",
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
        @PositiveOrZero(message = "Budget must be zero or positive")
        BigDecimal budget,

        @Schema(
                description = "ISO 4217 currency code",
                example = "EUR"
        )
        @Size(max = 10, message = "Currency code must not exceed 10 characters")
        String currency,

        @Schema(
                description = "URL for the trip cover image",
                example = "/uploads/cover.jpg"
        )
        @Size(max = 500, message = "Cover URL must not exceed 500 characters")
        String coverUrl,

        @Schema(
                description = "Whether the trip is publicly visible in catalog",
                example = "true"
        )
        Boolean isPublic
) {}
