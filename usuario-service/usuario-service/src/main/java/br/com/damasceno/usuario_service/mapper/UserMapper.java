package br.com.damasceno.usuario_service.mapper;

import br.com.damasceno.usuario_service.dto.UserRequestDto;
import br.com.damasceno.usuario_service.dto.UserResponseDto;
import br.com.damasceno.usuario_service.model.UserModel;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserModel toEntity(UserRequestDto dto) {
        UserModel user = new UserModel();
        user.setName(dto.name());
        user.setEmail(dto.email());
        return user;
    }

    public UserResponseDto toResponseDto(UserModel entity) {
        return new UserResponseDto(entity.getUserId(), entity.getName(), entity.getEmail());
    }
}
