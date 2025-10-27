package br.com.damasceno.email_service.service;

import br.com.damasceno.email_service.dto.EmailRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendWelcomeEmail(EmailRequestDto emailRequestDto) {
        log.info("==================================================");
        log.info("📧 E-mail de boas-vindas sendo enviado...");
        log.info("Para: {}", emailRequestDto.to());
        log.info("Assunto: {}", emailRequestDto.subject());
        log.info("Corpo: {}", emailRequestDto.body());
        log.info("✅ E-mail enviado com sucesso (simulado)!");
        log.info("==================================================");
    }
}

