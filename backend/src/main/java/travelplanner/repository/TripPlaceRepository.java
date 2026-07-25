package travelplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.TripPlace;
import java.util.UUID;

@Repository
public interface TripPlaceRepository extends JpaRepository<TripPlace, UUID> {
}
