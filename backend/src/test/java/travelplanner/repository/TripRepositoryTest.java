package travelplanner.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import travelplanner.entity.Trip;
import travelplanner.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TripRepositoryTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = entityManager.find(
                User.class,
                UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
    }

    @Test
    void findAllByOwner_UserId_ReturnsTripsForOwner() {
        Trip trip = new Trip();
        trip.setTitle("Owner Trip");
        trip.setDestination("Kyiv");
        trip.setOwner(testUser);
        trip.setIsPublic(false);
        entityManager.persist(trip);
        entityManager.flush();

        List<Trip> trips = tripRepository.findAllByOwner_UserId(testUser.getUserId());

        assertNotNull(trips);
        assertFalse(trips.isEmpty());
        assertTrue(trips.stream().anyMatch(t -> "Owner Trip".equals(t.getTitle())));
    }

    @Test
    void findAllByOwnerOrderByCreatedAtDesc_ReturnsPagedTrips() {
        Trip trip1 = new Trip();
        trip1.setTitle("First Trip");
        trip1.setOwner(testUser);
        trip1.setIsPublic(false);
        entityManager.persist(trip1);

        Trip trip2 = new Trip();
        trip2.setTitle("Second Trip");
        trip2.setOwner(testUser);
        trip2.setIsPublic(true);
        entityManager.persist(trip2);
        entityManager.flush();

        Page<Trip> page = tripRepository.findAllByOwnerOrderByCreatedAtDesc(
                testUser,
                PageRequest.of(0, 10)
        );

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    void findAllByIsPublicTrueOrderByCreatedAtDesc_ReturnsOnlyPublicTrips() {
        Trip publicTrip = new Trip();
        publicTrip.setTitle("Public Catalog Trip");
        publicTrip.setOwner(testUser);
        publicTrip.setIsPublic(true);
        entityManager.persist(publicTrip);

        Trip privateTrip = new Trip();
        privateTrip.setTitle("Private Hidden Trip");
        privateTrip.setOwner(testUser);
        privateTrip.setIsPublic(false);
        entityManager.persist(privateTrip);
        entityManager.flush();

        Page<Trip> page = tripRepository.findAllByIsPublicTrueOrderByCreatedAtDesc(
                PageRequest.of(0, 50)
        );

        assertNotNull(page);
        assertTrue(page.getContent().stream().allMatch(Trip::getIsPublic));
        assertTrue(page.getContent().stream()
                .anyMatch(t -> "Public Catalog Trip".equals(t.getTitle())));
        assertFalse(page.getContent().stream()
                .anyMatch(t -> "Private Hidden Trip".equals(t.getTitle())));
    }

    @Test
    void findAllByIsPublicTrueAndTitleContainingIgnoreCase_FiltersCorrectly() {
        Trip matchingTrip = new Trip();
        matchingTrip.setTitle("Unique Rome Discovery");
        matchingTrip.setOwner(testUser);
        matchingTrip.setIsPublic(true);
        entityManager.persist(matchingTrip);

        Trip nonMatchingTrip = new Trip();
        nonMatchingTrip.setTitle("Unique Tokyo Discovery");
        nonMatchingTrip.setOwner(testUser);
        nonMatchingTrip.setIsPublic(true);
        entityManager.persist(nonMatchingTrip);
        entityManager.flush();

        Page<Trip> page = tripRepository
                .findAllByIsPublicTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        "rome",
                        PageRequest.of(0, 10)
                );

        assertNotNull(page);
        assertTrue(page.getContent().stream()
                .anyMatch(t -> "Unique Rome Discovery".equals(t.getTitle())));
        assertFalse(page.getContent().stream()
                .anyMatch(t -> "Unique Tokyo Discovery".equals(t.getTitle())));
    }
}
