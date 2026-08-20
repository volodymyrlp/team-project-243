package travelplanner.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Itinerary;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
}
