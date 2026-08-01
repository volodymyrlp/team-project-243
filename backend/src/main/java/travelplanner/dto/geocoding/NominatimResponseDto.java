package travelplanner.dto.geocoding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NominatimResponseDto {
    private String lat;
    private String lon;

    @JsonProperty("display_name")
    private String displayName;
}
