package br.com.damasceno.email_service.dto;

public record EmailRequestDto(String to, String subject, String body) {
}
