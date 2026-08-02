package travelplanner.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travelplanner.dto.place.PlaceAddRequest;
import travelplanner.dto.place.PlaceResponse;
import travelplanner.entity.Place;
import travelplanner.service.PlaceService;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @PostMapping
    public ResponseEntity<PlaceResponse> addPlaceToTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody PlaceAddRequest request
    ) {
        Place place = placeService.addPlaceToTrip(tripId, request.getQuery());
        PlaceResponse response = mapToResponse(place);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private PlaceResponse mapToResponse(Place place) {
        PlaceResponse response = new PlaceResponse();
        response.setId(place.getPlaceId());
        response.setName(place.getName());
        response.setAddress(place.getAddress());
        response.setLatitude(place.getLatitude());
        response.setLongitude(place.getLongitude());
        return response;
    }
}
