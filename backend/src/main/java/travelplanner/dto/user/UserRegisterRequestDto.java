package travelplanner.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import travelplanner.annotation.FieldMatch;

@Data
@FieldMatch(field = "passwordHash", fieldMatch = "confirmPassword")
public class UserRegisterRequestDto {
    @NotBlank(message = "Full name is required")
    private String fullName;
    @NotBlank(message = "email is required")
    @Email
    private String email;
    @NotBlank(message = "password is required")
    @Length(min = 8, max = 20)
    private String passwordHash;
    @NotBlank(message = "confirm password is required")
    @Length(min = 8, max = 20)
    private String confirmPassword;
}
