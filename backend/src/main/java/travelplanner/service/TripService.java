package travelplanner.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Trip;
import travelplanner.entity.TripDay;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.mapper.TripMapper;
import travelplanner.repository.TripRepository;
import travelplanner.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMapper tripMapper;

    @Transactional
    public TripResponse createTrip(UUID userId, TripCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId));

        Trip trip = tripMapper.toEntity(request);
        trip.setOwner(user);

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        int dayNum = 1;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            TripDay day = new TripDay();
            day.setTrip(trip);
            day.setDayNumber(dayNum++);
            day.setDate(date);
            trip.getTripDays().add(day);
        }

        Trip savedTrip = tripRepository.save(trip);
        return tripMapper.toResponse(savedTrip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getUserTrips(UUID userId) {
        return tripRepository.findAllByOwner_UserId(userId).stream()
                .map(tripMapper::toResponse)
                .collect(Collectors.toList());
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
