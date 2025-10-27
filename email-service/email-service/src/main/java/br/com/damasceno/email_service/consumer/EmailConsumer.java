package br.com.damasceno.email_service.consumer;

import br.com.damasceno.email_service.dto.EmailRequestDto;
import br.com.damasceno.email_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final EmailService emailService;

    // A anotação @RabbitListener faz a mágica.
    // Ele se inscreve na fila definida no application.properties
    // e consome as mensagens assim que elas chegam.
    @RabbitListener(queues = "${spring.rabbitmq.queue.welcome}")
    public void consumeWelcomeEmail(EmailRequestDto emailRequestDto) {
        log.info("Mensagem recebida da fila: {}", emailRequestDto);
        try {
            emailService.sendWelcomeEmail(emailRequestDto);
            log.info("E-mail de boas vindas processado coom sucesso.");
        }catch (Exception e) {
            log.error("Erro de processar email de boas vindas {}", emailRequestDto, e);
        }
    }
}
