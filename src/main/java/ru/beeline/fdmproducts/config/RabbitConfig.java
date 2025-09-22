package ru.beeline.fdmproducts.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.beeline.fdmproducts.client.AuthSSOClient;

@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.username}")
    private String userName;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${queue.comparison-operations.name}")
    private String queueName;


    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingName;

    @Value("${spring.rabbitmq.host}")
    private String connectFactoryName;

    @Autowired
    private AuthSSOClient authSSOClient;

    @Bean
    public Queue queue() {
        return new Queue(queueName, true);
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        return createConnectionFactoryWithToken();
    }

    private CachingConnectionFactory createConnectionFactoryWithToken() {
        CachingConnectionFactory factory = new CachingConnectionFactory(connectFactoryName);
        factory.setUsername("");
        factory.setPassword(authSSOClient.getToken());
        factory.setVirtualHost(virtualHost);

        factory.addConnectionListener(new ConnectionListener() {
            @Override
            public void onCreate(Connection connection) {
                factory.setPassword(authSSOClient.getToken());
            }

            @Override
            public void onClose(Connection connection) {
            }
        });
        return factory;
    }

    @Bean
    MessageConverter messageConverter() {
        return new SimpleMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
