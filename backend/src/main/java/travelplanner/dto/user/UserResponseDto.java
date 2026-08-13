package travelplanner.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import travelplanner.entity.Trip;

public record UserResponseDto(
        UUID userId,
        String email,
        String fullName,
        LocalDateTime createdAt,
        String avatarUrl,
        List<Trip> trips
) {
}
