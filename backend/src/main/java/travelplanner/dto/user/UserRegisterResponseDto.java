package travelplanner.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import travelplanner.entity.Trip;

public record UserRegisterResponseDto(
        UUID userId,
        String email,
        String fullName,
        LocalDateTime createdAt,
        List<Trip> trips
) {
}
