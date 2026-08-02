package travelplanner.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.service.TripService;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TripCreateRequest request
    ) {
        TripResponse response = tripService.createTrip(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getUserTrips(@AuthenticationPrincipal User user) {
        List<TripResponse> response = tripService.getUserTrips(user.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable UUID tripId) {
        TripResponse response = tripService.getTripById(tripId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
