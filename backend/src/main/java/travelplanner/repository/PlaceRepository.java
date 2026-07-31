package travelplanner.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Place;

@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {
}
