package travelplanner.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TripResponse {
    private UUID tripId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}
