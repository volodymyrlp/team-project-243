package travelplanner.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import travelplanner.dto.trip.PhotoResponse;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Photo;
import travelplanner.entity.Trip;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "tripId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "tripDays", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Trip toEntity(TripCreateRequest request);

    TripResponse toResponse(Trip trip);

    List<TripResponse> toResponseList(List<Trip> trips);

    @Mapping(source = "uploader.userId", target = "uploaderId")
    PhotoResponse toPhotoResponse(Photo photo);

    List<PhotoResponse> toPhotoResponseList(List<Photo> photos);
}
