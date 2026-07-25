package travelplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Place;
import java.util.UUID;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {
}
