package travelplanner.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Trip;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByUser_UserId(UUID userId);
}
