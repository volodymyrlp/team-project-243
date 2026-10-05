package travelplanner.dto.user;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class UserResponseDto {
    private UUID userId;
    private String email;
    private String fullName;
    private LocalDateTime createdAt;
    private String avatarUrl;
}
