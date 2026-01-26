package com.project.chatapp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
public class JacksonConfig implements WebSocketMessageBrokerConfigurer {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Giúp Backend không bị crash nếu Android gửi thừa trường lạ
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Chấp nhận giá trị null cho các trường Boolean (isRevoked, isPinned)
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        return mapper;
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // Sử dụng ObjectMapper đã cấu hình ở trên cho WebSocket
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper());

        // Đưa converter này lên vị trí đầu tiên để ưu tiên xử lý JSON
        messageConverters.add(0, converter);
        return false;
    }
}