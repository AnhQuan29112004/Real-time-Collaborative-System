package com.collabnote.user.mapper;

import com.collabnote.common.config.CentralMapperConfig;
import com.collabnote.user.dto.UserResponse;
import com.collabnote.user.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {
    UserResponse toResponse(UserEntity entity);
}
