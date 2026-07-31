package travelplanner.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import travelplanner.config.MapperConfig;
import travelplanner.dto.user.UserRegisterRequestDto;
import travelplanner.dto.user.UserRegisterResponseDto;
import travelplanner.entity.User;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "trips", ignore = true)
    User toModel(UserRegisterRequestDto requestDto);

    UserRegisterResponseDto toDto(User user);
}
