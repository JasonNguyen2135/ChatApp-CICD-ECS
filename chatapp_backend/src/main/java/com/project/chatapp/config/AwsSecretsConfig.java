package com.project.chatapp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import jakarta.annotation.PostConstruct;

@Configuration
public class AwsSecretsConfig {

    @Value("${aws.secrets.arn:}")
    private String secretArn;

    @Value("${aws.region}")
    private String region;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadSecrets() {
        if (secretArn == null || secretArn.isEmpty()) {
            return;
        }

        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build()) {

            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretArn)
                    .build();

            GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
            String secret = valueResponse.secretString();
            
            JsonNode secretsJson = objectMapper.readTree(secret);
            
            // Đưa vào System properties để Spring Boot tự nhận diện cho Datasource và JWT
            System.setProperty("spring.datasource.password", secretsJson.get("DB_PASSWORD").asText());
            System.setProperty("jwt.secret", secretsJson.get("JWT_SECRET").asText());

        } catch (Exception e) {
            System.err.println("Lỗi load secrets từ AWS: " + e.getMessage());
        }
    }
}
