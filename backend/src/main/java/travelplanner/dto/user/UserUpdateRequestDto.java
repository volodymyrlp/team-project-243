package travelplanner.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequestDto {
    @NotBlank(message = "Full name is required")
    private String fullName;
    private String avatarUrl;
}
