package travelplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import travelplanner.dto.trip.PhotoResponse;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.service.TripService;

@Tag(
        name = "Trip Management",
        description = "Endpoints for managing trips, daily itineraries, covers, and photos"
)
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(
            summary = "Create a new trip",
            description = "Creates a new trip for current user and generates daily itineraries."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Trip created successfully",
                    content = @Content(schema = @Schema(implementation = TripResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or invalid dates",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied / unauthenticated",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody TripCreateRequest request
    ) {
        TripResponse response = tripService.createTrip(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get user trips",
            description = "Retrieves a paginated list of trips created by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User trips retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied / unauthenticated",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<Page<TripResponse>> getMyTrips(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<TripResponse> response = tripService.getMyTrips(user, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get public trip catalog",
            description = "Retrieves a paginated list of public trips with optional search."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Public catalog retrieved successfully"
            )
    })
    @GetMapping("/catalog")
    public ResponseEntity<Page<TripResponse>> getPublicTripCatalog(
            @Parameter(description = "Search keyword for trip title")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<TripResponse> response = tripService.getPublicTripCatalog(search, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get trip by ID",
            description = "Retrieves details of a specific trip by its unique identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Trip retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TripResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied / unauthenticated",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trip not found",
                    content = @Content
            )
    })
    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> getTripById(
            @Parameter(description = "Unique ID of the trip", required = true)
            @PathVariable UUID tripId
    ) {
        TripResponse response = tripService.getTripById(tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete trip",
            description = "Deletes a trip by its unique identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Trip deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied / unauthenticated",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trip not found",
                    content = @Content
            )
    })
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            @Parameter(description = "Unique ID of the trip to delete", required = true)
            @PathVariable UUID tripId
    ) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update trip cover",
            description = "Uploads and sets a new cover photo for the trip. Owner only."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cover updated successfully",
                    content = @Content(schema = @Schema(implementation = TripResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or empty upload",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user is not the trip owner",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trip not found",
                    content = @Content
            )
    })
    @PostMapping(value = "/{tripId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripResponse> updateTripCover(
            @Parameter(description = "Unique ID of the trip", required = true)
            @PathVariable UUID tripId,
            @Parameter(description = "Cover image file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        TripResponse response = tripService.updateTripCover(tripId, file, currentUser);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Upload trip photo",
            description = "Uploads a photo for the trip. Owner only."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Photo uploaded successfully",
                    content = @Content(schema = @Schema(implementation = PhotoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or empty upload",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user is not the trip owner",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trip not found",
                    content = @Content
            )
    })
    @PostMapping(value = "/{tripId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadTripPhoto(
            @Parameter(description = "Unique ID of the trip", required = true)
            @PathVariable UUID tripId,
            @Parameter(description = "Photo file to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        PhotoResponse response = tripService.uploadTripPhoto(tripId, file, currentUser);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get trip photos",
            description = "Retrieves all photos associated with the specified trip."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Photos retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied / unauthenticated",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trip not found",
                    content = @Content
            )
    })
    @GetMapping("/{tripId}/photos")
    public ResponseEntity<List<PhotoResponse>> getTripPhotos(
            @Parameter(description = "Unique ID of the trip", required = true)
            @PathVariable UUID tripId
    ) {
        List<PhotoResponse> response = tripService.getTripPhotos(tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Clone trip",
            description = "Clones a public trip or user's own private trip into a new trip."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Trip cloned successfully",
                    content = @Content(schema = @Schema(implementation = TripResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied to private trip",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Trip not found",
                    content = @Content
            )
    })
    @PostMapping("/{tripId}/clone")
    public ResponseEntity<TripResponse> cloneTrip(
            @Parameter(description = "Unique ID of the trip to clone", required = true)
            @PathVariable UUID tripId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser
    ) {
        TripResponse response = tripService.cloneTrip(tripId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
