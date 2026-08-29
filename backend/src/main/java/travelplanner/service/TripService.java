package travelplanner.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Trip;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.mapper.TripMapper;
import travelplanner.repository.TripRepository;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;

    @Transactional
    public TripResponse createTrip(TripCreateRequest request, User currentUser) {
        Trip trip = tripMapper.toEntity(request);
        trip.setOwner(currentUser);
        if (trip.getIsPublic() == null) {
            trip.setIsPublic(false);
        }

        Trip savedTrip = tripRepository.save(trip);
        return tripMapper.toResponse(savedTrip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getMyTrips(User currentUser, Pageable pageable) {
        Page<Trip> trips = tripRepository.findAllByOwnerOrderByCreatedAtDesc(currentUser, pageable);
        return trips.map(tripMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getPublicTripCatalog(String search, Pageable pageable) {
        Page<Trip> trips;
        if (search != null && !search.isBlank()) {
            trips = tripRepository
                    .findAllByIsPublicTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                            search, pageable);
        } else {
            trips = tripRepository.findAllByIsPublicTrueOrderByCreatedAtDesc(pageable);
        }
        return trips.map(tripMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new EntityNotFoundException("Trip not found with id: " + tripId));
        return tripMapper.toResponse(trip);
    }

    @Transactional
    public void deleteTrip(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new EntityNotFoundException("Trip not found with id: " + tripId);
        }
        tripRepository.deleteById(tripId);
    }
}
