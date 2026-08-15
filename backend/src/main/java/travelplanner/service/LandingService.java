package travelplanner.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import travelplanner.dto.landing.LandingContentResponse;
import travelplanner.entity.Country;
import travelplanner.entity.Review;
import travelplanner.repository.CountryRepository;
import travelplanner.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
public class LandingService {

    private final CountryRepository countryRepository;
    private final ReviewRepository reviewRepository;

    public LandingContentResponse getLandingContent() {
        List<Country> topCountries = countryRepository.findTop5ByOrderByPopularityScoreDesc();
        List<Review> topReviews = reviewRepository.findTop5ByOrderByCreatedAtDesc();

        List<LandingContentResponse.CountryDto> featuredDestinations = topCountries.stream()
                .map(country -> new LandingContentResponse.CountryDto(
                        country.getCountryId(),
                        country.getName(),
                        country.getPopularityScore()
                ))
                .toList();

        List<LandingContentResponse.ReviewDto> reviews = topReviews.stream()
                .map(review -> new LandingContentResponse.ReviewDto(
                        review.getReviewId(),
                        review.getAuthorName(),
                        review.getContent(),
                        review.getRating(),
                        review.getCreatedAt()
                ))
                .toList();

        return new LandingContentResponse(featuredDestinations, reviews);
    }
}
