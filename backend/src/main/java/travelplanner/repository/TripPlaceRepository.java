package travelplanner.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.TripPlace;

@Repository
public interface TripPlaceRepository extends JpaRepository<TripPlace, UUID> {
}
