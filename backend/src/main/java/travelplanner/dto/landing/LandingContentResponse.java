package travelplanner.dto.landing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LandingContentResponse(
        List<CountryDto> featuredDestinations,
        List<ReviewDto> reviews
) {
    public record CountryDto(
            UUID countryId,
            String name,
            Integer popularityScore
    ) {}

    public record ReviewDto(
            UUID reviewId,
            String authorName,
            String content,
            Integer rating,
            LocalDateTime createdAt
    ) {}
}
