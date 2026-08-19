package travelplanner.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import travelplanner.entity.Trip;

@Data
public class UserResponseDto {
    private UUID userId;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
    private String avatarUrl;
    private List<Trip> trips;
}
