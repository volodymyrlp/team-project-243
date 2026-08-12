package travelplanner.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travelplanner.entity.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {
    List<Country> findTop5ByOrderByPopularityScoreDesc();
}
