package br.com.damasceno.usuario_service.dto;

public record EmailRequestDto(String to, String subject, String body) {
}
