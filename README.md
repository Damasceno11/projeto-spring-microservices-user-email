# 🚀 Projeto: Microsserviços Spring Boot com RabbitMQ 🐇

Este projeto demonstra a construção de um sistema de microsserviços (Spring Boot) com foco em **desacoplamento**, **resiliência** e **comunicação assíncrona** orientada a eventos.

O objetivo principal é estudar a evolução de uma arquitetura de comunicação síncrona (baseada em `RestTemplate`) para uma arquitetura assíncrona robusta (usando `Spring AMQP` e `RabbitMQ`).

O sistema é composto por dois serviços independentes:
* **`usuario-service` (Porta 8080)**: Uma API RESTful principal, responsável pelo CRUD completo de usuários e pela **produção** de eventos.
* **`email-service` (Porta 8081)**: Um microserviço *worker*, desacoplado, responsável pelo **consumo** de eventos (simulando o envio de e-mails de boas-vindas).

## 🏛️ Arquitetura do Sistema

O diagrama abaixo ilustra o fluxo de dados assíncrono implementado. Quando um cliente cadastra um novo usuário, o `usuario-service` salva a informação no banco de dados e, em paralelo, publica uma mensagem em um *exchange* do RabbitMQ. O `email-service`, que está "ouvindo" a fila correta, consome esta mensagem e executa sua lógica de negócio (enviar o e-mail) sem que o serviço de usuário precise esperar por essa resposta.

```mermaid
graph TD
    subgraph Fluxo_de_Requisição
        Client[Cliente API (Ex: Postman)] -->|POST /users (JSON)| US[usuario-service 8080]
    end

    subgraph Serviço_de_Usuário_(Produtor)
        US -->|Salva no DB| DB[(PostgreSQL)]
        US -->|Publica Mensagem| RMQ[RabbitMQ Exchange]
    end

    subgraph Broker_de_Mensageria
        RMQ -->|Roteia| Queue[email.welcome.queue]
    end

    subgraph Serviço_de_Email_(Consumidor)
        Queue -->|Entrega Mensagem| ES[email-service 8081]
        ES -->|Processa (Simula Envio)| Log[Console Log]
    end

````

## 🛠️ Tecnologias Utilizadas

  * **Backend:** Java 17+, Spring Boot
  * **Mensageria:** Spring AMQP (RabbitMQ)
  * **Persistência:** Spring Data JPA, PostgreSQL
  * **Comunicação:** REST (CRUD) e AMQP (Eventos)
  * **Infraestrutura:** Docker (para RabbitMQ)
  * **Build:** Maven
  * **Utilitários:** Lombok

## 🎯 Desafio: De Síncrono (REST) para Assíncrono (RabbitMQ)

O principal desafio arquitetural deste projeto foi evoluir de um modelo de comunicação direta e bloqueante para um modelo orientado a eventos, resolvendo problemas de acoplamento e resiliência.

### ❌ Abordagem Inicial (Síncrona - *Problemática*)

A solução inicial (e problemática) seria o `usuario-service` chamar o `email-service` diretamente via `RestTemplate` (HTTP).

```java
// Abordagem Síncrona (Abandonada)
@Transactional
public UserResponseDto createUser(UserRequestDto dto) {
    // 1. Salva o usuário no banco
    UserModel savedUser = userRepository.save(userMapper.toEntity(dto));

    // 2. Chama o outro serviço via HTTP
    try {
        // PROBLEMA: O que acontece se o email-service estiver lento ou fora do ar?
        // O cadastro do usuário falha ou fica extremamente lento.
        EmailRequestDto emailRequest = new EmailRequestDto(...);
        restTemplate.postForEntity(emailServiceUrl, emailRequest, String.class);
    } catch (Exception e) {
        // O serviço de usuário agora precisa tratar falhas de outro serviço.
        log.error("Falha ao chamar email-service", e);
    }

    return userMapper.toResponseDto(savedUser);
}
```

Esta abordagem cria um **alto acoplamento** e reduz a **resiliência** do sistema.

### ✅ Solução Adotada (Assíncrona - *Implementada*)

Refatoramos a arquitetura para usar o padrão *Producer/Consumer* com RabbitMQ.

**1. O Produtor (`usuario-service`)** 🏭

O `UserService` agora apenas publica uma mensagem no *exchange*. Ele não sabe (e não se importa) quem irá consumi-la. A operação é instantânea e não bloqueante.

```java
// Solução Assíncrona (Adotada)
// Injetamos o RabbitTemplate e os nomes do exchange/routingkey
private final RabbitTemplate rabbitTemplate;
@Value("${spring.rabbitmq.exchange.direct}")
private String directExchangeName;
@Value("${spring.rabbitmq.routingkey.welcome}")
private String welcomeRoutingKey;

@Transactional
public UserResponseDto createUser(UserRequestDto userRequestDto) {
    // 1. Salva o usuário no banco
    UserModel savedUser = userRepository.save(userMapper.toEntity(userRequestDto));
    log.info("Usuário {} salvo com sucesso.", savedUser.getUserId());

    // 2. Prepara e ENVIA A MENSAGEM para o RabbitMQ
    try {
        EmailRequestDto emailRequest = new EmailRequestDto(savedUser.getEmail(), ...);
        
        // Apenas "dispara e esquece" (Fire-and-Forget)
        rabbitTemplate.convertAndSend(directExchangeName, welcomeRoutingKey, emailRequest);
        log.info("Mensagem enviada com sucesso!");

    } catch (Exception e) {
        log.error("ERRO ao enviar mensagem para RabbitMQ.", e);
    }

    // 3. Retorna para o cliente IMEDIATAMENTE
    return userMapper.toResponseDto(savedUser);
}
```

**2. O Consumidor (`email-service`)** 📬

O `email-service` não expõe mais uma API REST para esta funcionalidade. Em vez disso, ele usa um `Listener` que "escuta" ativamente a fila.

```java
// O Consumidor não tem Controller, apenas um Listener
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final EmailService emailService;

    // A mágica acontece aqui:
    // O Spring AMQP escuta a fila definida no .properties
    @RabbitListener(queues = "${spring.rabbitmq.queue.welcome}")
    public void consumeWelcomeEmail(EmailRequestDto emailRequestDto) {
        log.info("Mensagem recebida da fila: {}", emailRequestDto);
        // O serviço de email processa a mensagem no seu próprio tempo.
        emailService.sendWelcomeEmail(emailRequestDto);
    }
}
```

## 📐 Padrões de Arquitetura Aplicados

Seguindo as boas práticas *enterprise*:

  * **Camadas (Clean Architecture):** `Controller` -\> `Service` -\> `Repository`. A regra de negócio permanece isolada na camada de serviço.
  * **DTO (Data Transfer Object):** Utilizamos `UserRequestDto` e `UserResponseDto` (com Java Records) para que a entidade JPA (`UserModel`) nunca seja exposta na API REST.
  * **Mapper:** A conversão DTO \<=\> Entidade é isolada em uma classe `UserMapper`, mantendo o Princípio da Responsabilidade Única (SRP).
  * **Injeção de Dependência:** Utilizamos **injeção via construtor** (`@RequiredArgsConstructor` do Lombok) em vez de `@Autowired` em campos, garantindo a imutabilidade das dependências.

## 🏁 Como Executar

1.  **Pré-requisitos:**

      * Java 17 (ou superior)
      * Maven
      * Docker

2.  **Iniciar o RabbitMQ (via Docker):**

    ```bash
    docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
    ```

      * Interface de Gerenciamento: `http://localhost:15672` (login: `guest` / `guest`)

3.  **Configurar o Banco (PostgreSQL):**

      * Crie um banco de dados (ex: `db_usuarios`).
      * Atualize o `application.properties` do `usuario-service` com sua URL, usuário e senha do PostgreSQL.

4.  **Iniciar os Serviços:**

      * Execute a classe `UsuarioServiceApplication` (Porta 8080).
      * Execute a classe `EmailServiceApplication` (Porta 8081).

5.  **Testar:**

      * Use o Postman ou `curl` para enviar um `POST` para `http://localhost:8080/users` com um JSON:
        ```json
        {
          "name": "Pedro Paulo",
          "email": "pedropaulodamasceno@gmail.com"
        }
        ```

## 🔬 Prova de Conceito: Resiliência a Falhas

O maior benefício desta arquitetura é a resiliência. Realizamos um teste de "caos" para simular uma falha no serviço de e-mail.

**Cenário 1:** O `email-service` (Consumidor) foi **desligado**, mas o `usuario-service` (Produtor) continuou online recebendo cadastros.

**Resultado (Imagem 1):** O console do `usuario-service` mostra que ele continuou cadastrando usuários e publicando as mensagens no RabbitMQ sem qualquer erro ou espera. O cadastro do usuário foi concluído com sucesso, mesmo com o serviço de e-mail fora do ar.

<img width="1039" height="332" alt="Captura de tela 2025-10-21 211818" src="https://github.com/user-attachments/assets/1cb9ee40-18e1-496c-8d8c-e60ee595c123" />


**Cenário 2:** O `email-service` foi **religado** após um tempo.

**Resultado (Imagem 2):** O console do `email-service` mostra que, assim que ele iniciou, ele se conectou ao RabbitMQ e começou a processar **imediatamente** a fila de mensagens que estava acumulada (no exemplo, os e-mails para "Ana Feijó" e "Carlos Santos").

**Nenhuma mensagem foi perdida.** O sistema se "curou" sozinho, provando o desacoplamento e a resiliência da arquitetura.

<img width="1919" height="1077" alt="Captura de tela 2025-10-27 160937" src="https://github.com/user-attachments/assets/24b8a72e-b516-4cdf-b5fd-eda61be9fea8" />


```
```



