package com.project.chatapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatapp.model.ChatMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class RedisConfig {

    // ✅ Định nghĩa kênh chung để tất cả các Task cùng nghe
    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("chatapp-messages");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    // ✅ Cấu hình Listener: Khi Redis có tin nhắn mới, gọi hàm handleMessage
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, topic());
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageReceiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    // ✅ Component nhận tin nhắn từ Redis và đẩy xuống WebSocket của Task hiện tại
    @Configuration
    public static class RedisMessageReceiver {
        private final SimpMessagingTemplate messagingTemplate;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public RedisMessageReceiver(SimpMessagingTemplate messagingTemplate) {
            this.messagingTemplate = messagingTemplate;
        }

        public void receiveMessage(String message) {
            try {
                // Chuyển từ JSON text của Redis sang Object
                ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
                
                // Đẩy xuống máy người dùng (Nếu người dùng đó đang kết nối vào Task này)
                messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiverId(), chatMessage);
                messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getSenderId(), chatMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
