package travelplanner.dto.trip;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class TripResponse {
    private UUID tripId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}
