package travelplanner.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travelplanner.dto.geocoding.NominatimResponseDto;
import travelplanner.entity.Place;
import travelplanner.entity.Trip;
import travelplanner.entity.TripPlace;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.repository.PlaceRepository;
import travelplanner.repository.TripPlaceRepository;
import travelplanner.repository.TripRepository;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final NominatimClientService nominatimClientService;

    @Transactional
    public Place addPlaceToTrip(UUID tripId, String addressQuery) {
        List<NominatimResponseDto> geocodingResults =
                nominatimClientService.search(addressQuery);
        if (geocodingResults == null || geocodingResults.isEmpty()) {
            throw new EntityNotFoundException(
                    "No coordinates found for address: " + addressQuery);
        }
        NominatimResponseDto coordinates = geocodingResults.get(0);

        Place place = new Place();
        place.setName(addressQuery);
        place.setAddress(coordinates.getDisplayName());
        place.setLatitude(new BigDecimal(coordinates.getLat()));
        place.setLongitude(new BigDecimal(coordinates.getLon()));

        Place savedPlace = placeRepository.save(place);

        final Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trip not found with id: " + tripId));

        TripPlace tripPlace = new TripPlace();
        tripPlace.setTrip(trip);
        tripPlace.setPlace(savedPlace);
        tripPlace.setDayNumber(1);
        tripPlace.setOrderIndex(1);
        tripPlaceRepository.save(tripPlace);

        return savedPlace;
    }
}
