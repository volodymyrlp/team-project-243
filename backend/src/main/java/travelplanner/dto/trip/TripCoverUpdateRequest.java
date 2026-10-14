package travelplanner.dto.trip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating a trip cover")
public record TripCoverUpdateRequest(
        @Schema(
                description = "Storage key for the trip cover image",
                example = "trip-covers/d3b07384-d113-4944-9cbe-123456789abc/photo.jpg"
        )
        @NotBlank(message = "Cover key cannot be blank")
        @Size(max = 500, message = "Cover key must not exceed 500 characters")
        @JsonProperty("coverKey")
        @JsonAlias("cover_key")
        String coverKey
) {}
