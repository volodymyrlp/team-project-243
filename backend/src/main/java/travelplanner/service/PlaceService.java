package travelplanner.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travelplanner.dto.geocoding.NominatimResponseDto;
import travelplanner.entity.Itinerary;
import travelplanner.entity.Place;
import travelplanner.entity.Trip;
import travelplanner.entity.TripDay;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.repository.PlaceRepository;
import travelplanner.repository.TripRepository;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final TripRepository tripRepository;
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

        TripDay tripDay = trip.getTripDays().stream()
                .filter(d -> d.getDayNumber() == 1)
                .findFirst()
                .orElseGet(() -> {
                    TripDay newDay = new TripDay();
                    newDay.setTrip(trip);
                    newDay.setDayNumber(1);
                    newDay.setDate(null);
                    trip.getTripDays().add(newDay);
                    return newDay;
                });

        Itinerary itinerary = new Itinerary();
        itinerary.setTripDay(tripDay);
        itinerary.setPlace(savedPlace);
        itinerary.setVisitOrder(1);
        itinerary.setNotes(null);
        itinerary.setTimeSpentMinutes(null);
        tripDay.getItineraries().add(itinerary);

        tripRepository.save(trip);

        return savedPlace;
    }
}
