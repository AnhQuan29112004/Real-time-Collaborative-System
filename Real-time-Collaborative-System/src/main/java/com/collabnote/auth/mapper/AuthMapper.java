package com.collabnote.auth.mapper;
import com.collabnote.auth.entity.AuthSessionEntity;
import com.collabnote.auth.dto.response.SessionResponse;
import com.collabnote.common.config.CentralMapperConfig;
import org.mapstruct.*;
@Mapper(config = CentralMapperConfig.class)
public interface AuthMapper {
    SessionResponse toResponse(AuthSessionEntity entity, boolean current);
}
