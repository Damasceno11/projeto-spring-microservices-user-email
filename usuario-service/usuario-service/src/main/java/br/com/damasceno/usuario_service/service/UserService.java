package br.com.damasceno.usuario_service.service;

import br.com.damasceno.usuario_service.dto.EmailRequestDto;
import br.com.damasceno.usuario_service.dto.UserRequestDto;
import br.com.damasceno.usuario_service.dto.UserResponseDto;
import br.com.damasceno.usuario_service.mapper.UserMapper;
import br.com.damasceno.usuario_service.model.UserModel;
import br.com.damasceno.usuario_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchange.direct}")
    private String directExchangeName;

    @Value("${spring.rabbitmq.routingkey.welcome}")
    private String welcomeRoutingKey;

    @Transactional
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        if (userRequestDto.name() == null || userRequestDto.email() == null) {
            throw new IllegalArgumentException("Nome e e-mail são obrigatórios.");
        }

        UserModel user = userMapper.toEntity(userRequestDto);
        UserModel savedUser = userRepository.save(user);
        log.info("Usuário {} salvo com sucesso.", savedUser.getUserId());

        try {
            String subject = "Bem-vindo ao nosso sistema!";
            String body = "Olá, " + savedUser.getName() + "! Seu cadastro foi realizado com sucesso.";
            EmailRequestDto emailRequest = new EmailRequestDto(savedUser.getEmail(), subject, body);

            log.info("Enviando mensagem para a fila de boas-vindas...");

            rabbitTemplate.convertAndSend(directExchangeName, welcomeRoutingKey, emailRequest);

            log.info("Mensagem enviada com sucesso.");

        }catch (Exception e) {
            log.error("ERRO ao enviar mensagem para RabbitMQ. Usuário criado", e);
        }

        return userMapper.toResponseDto(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> findAllUsers(){
        return userRepository.findAll().stream()
                .map(userMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDto findUserById(UUID userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + userId));
        return userMapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateUser(UUID userId, UserRequestDto userRequestDto) {
        UserModel userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + userId));

        if (userRequestDto.name() == null || userRequestDto.email() == null) {
            throw new IllegalArgumentException("Nome e e-mail são obrigatórios.");
        }

        userToUpdate.setName(userRequestDto.name());
        userToUpdate.setEmail(userRequestDto.email());

        UserModel updatedUser = userRepository.save(userToUpdate);
        return userMapper.toResponseDto(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Usuário não encontrado com ID: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("Usuário com ID {} deletado com sucesso", userId);
    }
}
























