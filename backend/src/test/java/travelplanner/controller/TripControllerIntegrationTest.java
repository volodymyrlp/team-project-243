package travelplanner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import travelplanner.dto.trip.PhotoResponse;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.User;
import travelplanner.service.TripService;

@SpringBootTest
@AutoConfigureMockMvc
class TripControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

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
}
