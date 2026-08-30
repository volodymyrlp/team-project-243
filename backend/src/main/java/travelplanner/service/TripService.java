package travelplanner.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import travelplanner.dto.trip.PhotoResponse;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Photo;
import travelplanner.entity.Trip;
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
    private final FileStorageService fileStorageService;
    private final PhotoRepository photoRepository;

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

    @Transactional
    public TripResponse updateTripCover(UUID tripId, MultipartFile file, User currentUser) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new EntityNotFoundException("Trip not found with id: " + tripId));

        if (!trip.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new ForbiddenException("You do not have permission to update this trip");
        }

        String coverUrl = fileStorageService.uploadFile(file);
        trip.setCoverUrl(coverUrl);
        Trip savedTrip = tripRepository.save(trip);
        return tripMapper.toResponse(savedTrip);
    }

    @Transactional
    public PhotoResponse uploadTripPhoto(UUID tripId, MultipartFile file, User currentUser) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(
                () -> new EntityNotFoundException("Trip not found with id: " + tripId));

        if (!trip.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new ForbiddenException("You do not have permission to modify this trip");
        }

        String photoUrl = fileStorageService.uploadFile(file);

        Photo photo = new Photo();
        photo.setTrip(trip);
        photo.setUploader(currentUser);
        photo.setUrl(photoUrl);

        Photo savedPhoto = photoRepository.save(photo);
        return tripMapper.toPhotoResponse(savedPhoto);
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getTripPhotos(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new EntityNotFoundException("Trip not found with id: " + tripId);
        }
        List<Photo> photos = photoRepository.findAllByTripTripId(tripId);
        return tripMapper.toPhotoResponseList(photos);
    }
}
