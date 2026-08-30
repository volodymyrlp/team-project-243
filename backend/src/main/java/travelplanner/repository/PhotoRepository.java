package travelplanner.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Photo;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findAllByTripTripId(UUID tripId);
}
