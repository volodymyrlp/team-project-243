package travelplanner.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TripCreateRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,

        @PositiveOrZero(message = "Budget must be zero or positive")
        BigDecimal budget,

        @Size(max = 10, message = "Currency code must not exceed 10 characters")
        String currency,

        @Size(max = 500, message = "Cover URL must not exceed 500 characters")
        String coverUrl,

        Boolean isPublic
) {}
