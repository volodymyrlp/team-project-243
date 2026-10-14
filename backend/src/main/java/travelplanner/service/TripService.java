package travelplanner.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travelplanner.dto.trip.PhotoResponse;
import travelplanner.dto.trip.TripCoverUpdateRequest;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Itinerary;
import travelplanner.entity.Photo;
import travelplanner.entity.Trip;
import travelplanner.entity.TripDay;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.exception.ForbiddenException;
import travelplanner.mapper.TripMapper;
import travelplanner.repository.PhotoRepository;
import travelplanner.repository.TripRepository;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final StorageService storageService;
    private final PhotoRepository photoRepository;

    @Transactional
    public TripResponse createTrip(TripCreateRequest request, User currentUser) {
        Trip trip = tripMapper.toEntity(request);
        trip.setOwner(currentUser);
        if (trip.getIsPublic() == null) {
            trip.setIsPublic(false);
        }

        if (trip.getStartDate() != null && trip.getEndDate() != null
                && !trip.getStartDate().isAfter(trip.getEndDate())) {
            long days = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate());
            for (int i = 0; i <= days; i++) {
                TripDay tripDay = new TripDay();
                tripDay.setTrip(trip);
                tripDay.setDayNumber(i + 1);
                tripDay.setDate(trip.getStartDate().plusDays(i));
                trip.getTripDays().add(tripDay);
            }
        }

        Trip savedTrip = tripRepository.save(trip);
        return toTripResponse(savedTrip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getMyTrips(User currentUser, Pageable pageable) {
        Page<Trip> trips = tripRepository
                .findAllByOwnerOrderByCreatedAtDesc(currentUser, pageable);
        return trips.map(this::toTripResponse);
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
        return trips.map(this::toTripResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new EntityNotFoundException("Trip not found with id: " + tripId));
        return toTripResponse(trip);
    }

    @Transactional
    public void deleteTrip(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new EntityNotFoundException("Trip not found with id: " + tripId);
        }
        tripRepository.deleteById(tripId);
    }

    @Transactional
    public TripResponse updateTripCover(
            UUID tripId,
            TripCoverUpdateRequest request,
            User currentUser
    ) {
        if (request == null || request.coverKey() == null || request.coverKey().isBlank()) {
            throw new IllegalArgumentException("Cover key cannot be blank");
        }

        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new EntityNotFoundException("Trip not found with id: " + tripId));

        if (!trip.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new ForbiddenException("You do not have permission to update this trip");
        }

        trip.setCoverUrl(request.coverKey());
        Trip savedTrip = tripRepository.save(trip);
        return toTripResponse(savedTrip);
    }

    @Transactional
    public TripResponse updateTripCover(UUID tripId, String coverKey, User currentUser) {
        return updateTripCover(tripId, new TripCoverUpdateRequest(coverKey), currentUser);
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getTripPhotos(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new EntityNotFoundException("Trip not found with id: " + tripId);
        }
        List<Photo> photos = photoRepository.findAllByTripTripId(tripId);
        return tripMapper.toPhotoResponseList(photos);
    }

    @Transactional
    public TripResponse cloneTrip(UUID originalTripId, User currentUser) {
        Trip originalTrip = tripRepository.findById(originalTripId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Trip not found with id: " + originalTripId));

        if (!originalTrip.getIsPublic()
                && !originalTrip.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You do not have access to this trip");
        }

        Trip clonedTrip = new Trip();
        clonedTrip.setTitle(originalTrip.getTitle() + " (Copy)");
        clonedTrip.setDestination(originalTrip.getDestination());
        clonedTrip.setStartDate(originalTrip.getStartDate());
        clonedTrip.setEndDate(originalTrip.getEndDate());
        clonedTrip.setDescription(originalTrip.getDescription());
        clonedTrip.setBudget(originalTrip.getBudget());
        clonedTrip.setCurrency(originalTrip.getCurrency());
        clonedTrip.setCoverUrl(originalTrip.getCoverUrl());
        clonedTrip.setOwner(currentUser);
        clonedTrip.setIsPublic(false);
        clonedTrip.setTags(new java.util.HashSet<>(originalTrip.getTags()));

        for (TripDay originalDay : originalTrip.getTripDays()) {
            TripDay clonedDay = new TripDay();
            clonedDay.setTrip(clonedTrip);
            clonedDay.setDayNumber(originalDay.getDayNumber());
            clonedDay.setDate(originalDay.getDate());

            for (Itinerary originalItinerary : originalDay.getItineraries()) {
                Itinerary clonedItinerary = new Itinerary();
                clonedItinerary.setTripDay(clonedDay);
                clonedItinerary.setVisitOrder(originalItinerary.getVisitOrder());
                clonedItinerary.setNotes(originalItinerary.getNotes());
                clonedItinerary.setTimeSpentMinutes(originalItinerary.getTimeSpentMinutes());
                clonedItinerary.setPlace(originalItinerary.getPlace());
                clonedDay.getItineraries().add(clonedItinerary);
            }

            clonedTrip.getTripDays().add(clonedDay);
        }

        Trip savedTrip = tripRepository.save(clonedTrip);
        return toTripResponse(savedTrip);
    }

    private TripResponse toTripResponse(Trip trip) {
        TripResponse response = tripMapper.toResponse(trip);
        if (response == null) {
            return null;
        }
        String coverUrl = trip != null ? trip.getCoverUrl() : response.coverUrl();
        if (coverUrl != null && !coverUrl.isBlank() && isSupabaseKey(coverUrl)) {
            String presignedUrl = storageService.generateGetPresignedUrl(coverUrl);
            if (presignedUrl != null) {
                return new TripResponse(
                        response.tripId(),
                        response.title(),
                        response.destination(),
                        response.startDate(),
                        response.endDate(),
                        response.description(),
                        response.budget(),
                        response.currency(),
                        presignedUrl,
                        response.isPublic(),
                        response.createdAt()
                );
            }
        }
        return response;
    }

    private boolean isSupabaseKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return !key.contains("://") && !key.startsWith("/");
    }
}
