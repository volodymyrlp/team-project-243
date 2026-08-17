package travelplanner.mapper;

import java.time.LocalDate;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Trip;
import travelplanner.entity.TripDay;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "tripId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "tripDays", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "budget", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "isPublic", ignore = true)
    Trip toEntity(TripCreateRequest request);

    @Mapping(target = "startDate", expression = "java(getStartDate(trip))")
    @Mapping(target = "endDate", expression = "java(getEndDate(trip))")
    TripResponse toResponse(Trip trip);

    default LocalDate getStartDate(Trip trip) {
        if (trip.getTripDays() == null || trip.getTripDays().isEmpty()) {
            return null;
        }
        return trip.getTripDays().stream()
                .map(TripDay::getDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    default LocalDate getEndDate(Trip trip) {
        if (trip.getTripDays() == null || trip.getTripDays().isEmpty()) {
            return null;
        }
        return trip.getTripDays().stream()
                .map(TripDay::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
    }
}
