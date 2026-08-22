package travelplanner.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Trip;
import travelplanner.entity.User;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByOwner_UserId(UUID userId);

    Page<Trip> findAllByOwnerOrderByCreatedAtDesc(User owner, Pageable pageable);

    Page<Trip> findAllByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Trip> findAllByIsPublicTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String title, Pageable pageable);
}
