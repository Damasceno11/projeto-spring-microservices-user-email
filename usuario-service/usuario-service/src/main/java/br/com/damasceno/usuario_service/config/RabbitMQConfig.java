package br.com.damasceno.usuario_service.config;


import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.queue.welcome}")
    private String welcomeQueueName;

    @Value("${spring.rabbitmq.exchange.direct}")
    private String directExchangeName;

    @Value("${spring.rabbitmq.routingkey.welcome}")
    private String welcomeRoutingKey;

    // Bean para a Fila de Boas-Vindas
    @Bean
    public Queue welcomeQueue() {
        return new Queue(welcomeQueueName, true);
    }

    // Bean para Exchange Direto
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(directExchangeName);
    }

    // Bean para Binding (Ligação)
    @Bean
    public Binding welcomeBinding(Queue welcomeQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(welcomeQueue).to(directExchange).with(welcomeRoutingKey);
    }

    // Bean parea o Message Converter (Serializar DTOs para JSON)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return  new Jackson2JsonMessageConverter();
    }

    // Bean para o RabbitTemplate (facilitador para enviar mensagens)
    @Bean
    public RabbitTemplate rabbitTamplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

}
