package travelplanner.dto.place;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceAddRequest {
    @NotBlank(message = "Search query must not be blank")
    private String query;
}
