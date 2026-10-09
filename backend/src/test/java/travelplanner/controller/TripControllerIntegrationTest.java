package travelplanner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import travelplanner.config.SecurityConfig;
import travelplanner.dto.trip.PhotoResponse;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.exception.ForbiddenException;
import travelplanner.security.CustomUserDetailsService;
import travelplanner.security.JwtUtil;
import travelplanner.service.TripService;

@WebMvcTest(TripController.class)
@Import(SecurityConfig.class)
class TripControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void uploadCover_Success() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "some-image-data".getBytes()
        );

        TripResponse response = new TripResponse(
                tripId,
                "My Trip",
                "Paris",
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                "Description",
                BigDecimal.valueOf(1000),
                "USD",
                "/uploads/test.jpg",
                false,
                LocalDateTime.now()
        );

        when(tripService.updateTripCover(eq(tripId), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/cover", tripId)
                        .file(file)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverUrl").value("/uploads/test.jpg"));
    }

    @Test
    void uploadCover_Forbidden_Returns403() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "some-image-data".getBytes()
        );

        when(tripService.updateTripCover(eq(tripId), any(), any()))
                .thenThrow(new ForbiddenException(
                        "You do not have permission to update this trip"));

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/cover", tripId)
                        .file(file)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadCover_NotFound_Returns404() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "some-image-data".getBytes()
        );

        when(tripService.updateTripCover(eq(tripId), any(), any()))
                .thenThrow(new EntityNotFoundException("Trip not found with id: " + tripId));

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/cover", tripId)
                        .file(file)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Trip not found with id: " + tripId));
    }

    @Test
    void uploadPhoto_Success() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "some-photo-data".getBytes()
        );

        PhotoResponse response = new PhotoResponse(
                UUID.randomUUID(),
                "/uploads/photo.jpg",
                currentUser.getUserId(),
                LocalDateTime.now()
        );

        when(tripService.uploadTripPhoto(eq(tripId), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/photos", tripId)
                        .file(file)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/photo.jpg"))
                .andExpect(jsonPath("$.uploaderId").value(currentUser.getUserId().toString()));
    }

    @Test
    void uploadPhoto_Forbidden_Returns403() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "some-photo-data".getBytes()
        );

        when(tripService.uploadTripPhoto(eq(tripId), any(), any()))
                .thenThrow(new ForbiddenException(
                        "You do not have permission to modify this trip"));

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/photos", tripId)
                        .file(file)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadPhoto_NotFound_Returns404() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "some-photo-data".getBytes()
        );

        when(tripService.uploadTripPhoto(eq(tripId), any(), any()))
                .thenThrow(new EntityNotFoundException("Trip not found with id: " + tripId));

        mockMvc.perform(multipart("/api/v1/trips/{tripId}/photos", tripId)
                        .file(file)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPhotos_Success() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        PhotoResponse response = new PhotoResponse(
                UUID.randomUUID(),
                "/uploads/photo.jpg",
                currentUser.getUserId(),
                LocalDateTime.now()
        );

        when(tripService.getTripPhotos(eq(tripId))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/trips/{tripId}/photos", tripId)
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("/uploads/photo.jpg"))
                .andExpect(jsonPath("$[0].uploaderId").value(currentUser.getUserId().toString()));
    }

    @Test
    void getPhotos_NotFound_Returns404() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        when(tripService.getTripPhotos(eq(tripId)))
                .thenThrow(new EntityNotFoundException("Trip not found with id: " + tripId));

        mockMvc.perform(get("/api/v1/trips/{tripId}/photos", tripId)
                        .with(user(currentUser)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Trip not found with id: " + tripId));
    }

    @Test
    void cloneTrip_Success() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        TripResponse response = new TripResponse(
                UUID.randomUUID(),
                "My Trip (Copy)",
                "Paris",
                LocalDate.now(),
                LocalDate.now().plusDays(5),
                "Description",
                BigDecimal.valueOf(1000),
                "USD",
                "/uploads/test.jpg",
                false,
                LocalDateTime.now()
        );

        when(tripService.cloneTrip(eq(tripId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trips/{tripId}/clone", tripId)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Trip (Copy)"))
                .andExpect(jsonPath("$.destination").value("Paris"))
                .andExpect(jsonPath("$.isPublic").value(false));
    }

    @Test
    void cloneTrip_Forbidden_Returns403() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        when(tripService.cloneTrip(eq(tripId), any()))
                .thenThrow(new AccessDeniedException("You do not have access to this trip"));

        mockMvc.perform(post("/api/v1/trips/{tripId}/clone", tripId)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cloneTrip_NotFound_Returns404() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        when(tripService.cloneTrip(eq(tripId), any()))
                .thenThrow(new EntityNotFoundException("Trip not found with id: " + tripId));

        mockMvc.perform(post("/api/v1/trips/{tripId}/clone", tripId)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTrip_Success() throws Exception {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        TripResponse response = new TripResponse(
                UUID.randomUUID(),
                "Rome Adventure",
                "Rome",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "Trip to Rome",
                BigDecimal.valueOf(1200),
                "EUR",
                null,
                false,
                LocalDateTime.now()
        );

        when(tripService.createTrip(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Rome Adventure",
                                  "destination": "Rome",
                                  "startDate": "2026-10-01",
                                  "endDate": "2026-10-05",
                                  "description": "Trip to Rome",
                                  "budget": 1200,
                                  "currency": "EUR",
                                  "isPublic": false
                                }
                                """)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Rome Adventure"))
                .andExpect(jsonPath("$.destination").value("Rome"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.endDate").value("2026-10-05"));
    }

    @Test
    void createTrip_ValidationFailure_BlankTitle_Returns400() throws Exception {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        mockMvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "destination": "Rome"
                                }
                                """)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTrip_ValidationFailure_NegativeBudget_Returns400() throws Exception {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        mockMvc.perform(post("/api/v1/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Budget Trip",
                                  "budget": -100
                                }
                                """)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyTrips_Success_WithPagination() throws Exception {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        TripResponse tripResponse = new TripResponse(
                UUID.randomUUID(),
                "User Trip",
                "Kyiv",
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                LocalDateTime.now()
        );

        Page<TripResponse> page = new PageImpl<>(
                List.of(tripResponse),
                PageRequest.of(1, 5),
                6
        );

        when(tripService.getMyTrips(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/trips?page=1&size=5")
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("User Trip"))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void getPublicTripCatalog_Success_WithSearchAndPagination() throws Exception {
        TripResponse tripResponse = new TripResponse(
                UUID.randomUUID(),
                "Rome Discovery",
                "Rome",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                LocalDateTime.now()
        );

        Page<TripResponse> page = new PageImpl<>(
                List.of(tripResponse),
                PageRequest.of(0, 10),
                1
        );

        when(tripService.getPublicTripCatalog(eq("Rome"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/trips/catalog")
                        .param("search", "Rome")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Rome Discovery"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPublicTripCatalog_Success_WithoutSearch() throws Exception {
        TripResponse tripResponse = new TripResponse(
                UUID.randomUUID(),
                "Any Trip",
                "Berlin",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                LocalDateTime.now()
        );

        Page<TripResponse> page = new PageImpl<>(
                List.of(tripResponse),
                PageRequest.of(0, 10),
                1
        );

        when(tripService.getPublicTripCatalog(eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/trips/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Any Trip"));
    }

    @Test
    void getTripById_Success() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        TripResponse tripResponse = new TripResponse(
                tripId,
                "Detailed Trip",
                "London",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                LocalDateTime.now()
        );

        when(tripService.getTripById(tripId)).thenReturn(tripResponse);

        mockMvc.perform(get("/api/v1/trips/{tripId}", tripId)
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Detailed Trip"))
                .andExpect(jsonPath("$.destination").value("London"));
    }

    @Test
    void getTripById_NotFound_Returns404() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        when(tripService.getTripById(tripId))
                .thenThrow(new EntityNotFoundException("Trip not found with id: " + tripId));

        mockMvc.perform(get("/api/v1/trips/{tripId}", tripId)
                        .with(user(currentUser)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Trip not found with id: " + tripId));
    }

    @Test
    void deleteTrip_Success() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        mockMvc.perform(delete("/api/v1/trips/{tripId}", tripId)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(tripService).deleteTrip(tripId);
    }

    @Test
    void deleteTrip_NotFound_Returns404() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        doThrow(new EntityNotFoundException("Trip not found with id: " + tripId))
                .when(tripService).deleteTrip(tripId);

        mockMvc.perform(delete("/api/v1/trips/{tripId}", tripId)
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Trip not found with id: " + tripId));
    }

    @Test
    void handleBadRequest_IllegalArgumentException_Returns400() throws Exception {
        UUID tripId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());
        currentUser.setEmail("test@example.com");

        when(tripService.getTripById(tripId))
                .thenThrow(new IllegalArgumentException("Invalid trip parameter"));

        mockMvc.perform(get("/api/v1/trips/{tripId}", tripId)
                        .with(user(currentUser)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid trip parameter"));
    }
}
