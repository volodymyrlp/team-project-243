package travelplanner.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import travelplanner.dto.TripCreateRequest;
import travelplanner.dto.TripResponse;
import travelplanner.entity.Trip;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "tripId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tripPlaces", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Trip toEntity(TripCreateRequest request);

    TripResponse toResponse(Trip trip);
}
