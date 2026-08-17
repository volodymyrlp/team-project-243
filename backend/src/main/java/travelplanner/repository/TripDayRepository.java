package travelplanner.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.TripDay;

@Repository
public interface TripDayRepository extends JpaRepository<TripDay, UUID> {
}
