package travelplanner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_places")
@Getter
@Setter
@NoArgsConstructor
public class TripPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trip_place_id", columnDefinition = "VARCHAR(36)", nullable = false)
    private UUID tripPlaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
