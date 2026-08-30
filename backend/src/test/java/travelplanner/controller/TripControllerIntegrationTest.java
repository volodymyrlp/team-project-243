package travelplanner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
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
}
