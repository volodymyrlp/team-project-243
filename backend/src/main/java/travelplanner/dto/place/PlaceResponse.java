package travelplanner.dto.place;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceResponse {
    private UUID id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
