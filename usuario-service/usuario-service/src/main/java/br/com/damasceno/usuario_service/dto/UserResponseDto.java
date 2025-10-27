package br.com.damasceno.usuario_service.dto;

import java.util.UUID;

public record UserResponseDto(UUID userId, String name, String email) {
}
