# 🔧 Configuration Guide - Logistics Intelligence Platform

## 📋 Overview

This guide explains how to configure the Logistics Intelligence Platform using various `application.yml` files for different environments and modules.

## 📁 Configuration File Structure

```
DPD-UK/
├── application.yml                    # Global configuration
├── application-dev.yml               # Development environment
├── application-prod.yml              # Production environment
├── application-test.yml              # Test environment
├── application-docker.yml            # Docker Compose environment
├── application-k8s.yml               # Kubernetes environment
├── microservices/
│   ├── eta-service/
│   │   └── src/main/resources/
│   │       └── application.yml       # ETA service specific config
│   ├── routing-service/
│   │   └── src/main/resources/
│   │       └── application.yml       # Routing service specific config
│   ├── returns-service/
│   │   └── src/main/resources/
│   │       └── application.yml       # Returns service specific config
│   ├── notification-service/
│   │   └── src/main/resources/
│   │       └── application.yml       # Notification service specific config
│   └── api-gateway/
│       └── src/main/resources/
│           └── application.yml       # API Gateway specific config
├── shared/
│   └── common-lib/
│       └── src/main/resources/
│           ├── application.yml       # Common library config
│           └── messages/
│               └── validation-messages.properties
└── frontend/
    └── ops-console/
        └── src/config/
            └── application.yml       # Frontend configuration
```

## 🌍 Environment Profiles

### 1. Development Profile (`dev`)

**File**: `application-dev.yml`

```yaml
spring:
  profiles:
    active: dev
  
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics_dev
    redis:
      host: localhost
      port: 6379
      database: 0

logging:
  level:
    com.dpd.uk: DEBUG
    org.springframework.cloud: DEBUG
```

**Usage**:
```bash
# Run with development profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Or set environment variable
export SPRING_PROFILES_ACTIVE=dev
```

### 2. Production Profile (`prod`)

**File**: `application-prod.yml`

```yaml
spring:
  profiles:
    active: prod
  
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://mongodb:27017/logistics}
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}

logging:
  level:
    com.dpd.uk: INFO
    org.springframework.cloud: INFO
  file:
    name: /var/log/logistics-platform/application.log
```

**Usage**:
```bash
# Run with production profile
mvn spring-boot:run -Dspring.profiles.active=prod

# Or set environment variable
export SPRING_PROFILES_ACTIVE=prod
```

### 3. Test Profile (`test`)

**File**: `application-test.yml`

```yaml
spring:
  profiles:
    active: test
  
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics_test
    redis:
      host: localhost
      port: 6379
      database: 1

logging:
  level:
    com.dpd.uk: DEBUG
    org.springframework.test: DEBUG
```

**Usage**:
```bash
# Run tests with test profile
mvn test -Dspring.profiles.active=test
```

### 4. Docker Profile (`docker`)

**File**: `application-docker.yml`

```yaml
spring:
  profiles:
    active: docker
  
  data:
    mongodb:
      uri: mongodb://mongodb:27017/logistics
    redis:
      host: redis
      port: 6379

  cloud:
    stream:
      kafka:
        binder:
          brokers: kafka:9092
```

**Usage**:
```bash
# Run with Docker Compose
docker-compose up -d
```

### 5. Kubernetes Profile (`k8s`)

**File**: `application-k8s.yml`

```yaml
spring:
  profiles:
    active: k8s
  
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://mongodb:27017/logistics}
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}

  cloud:
    stream:
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:kafka:9092}
```

**Usage**:
```bash
# Deploy to Kubernetes
kubectl apply -f k8s/
```

## 🔧 Module-Specific Configurations

### ETA Service Configuration

**File**: `microservices/eta-service/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: eta-service
  
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics_eta
    redis:
      host: localhost
      port: 6379
      database: 0

eta:
  calculation:
    default-confidence: MEDIUM
    max-prediction-hours: 24
    cache-ttl-minutes: 15
  factors:
    traffic-weight: 0.4
    distance-weight: 0.3
    historical-weight: 0.2
    depot-constraints-weight: 0.1
```

### Routing Service Configuration

**File**: `microservices/routing-service/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: routing-service
  
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics_routing
    redis:
      host: localhost
      port: 6379
      database: 1

routing:
  optimization:
    algorithm: GENETIC_ALGORITHM
    max-iterations: 1000
    population-size: 50
  constraints:
    max-route-duration-hours: 8
    max-route-distance-km: 200
```

### Returns Service Configuration

**File**: `microservices/returns-service/src/main/resources/application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: returns-service
  
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics_returns
    redis:
      host: localhost
      port: 6379
      database: 2

returns:
  ttl:
    default-hours: 24
    max-hours: 72
    warning-hours: 2
  approval:
    auto-approve-threshold: 50.0
    manual-review-threshold: 100.0
```

### Notification Service Configuration

**File**: `microservices/notification-service/src/main/resources/application.yml`

```yaml
server:
  port: 8084

spring:
  application:
    name: notification-service

websocket:
  allowed-origins: "*"
  max-connections: 1000
  heartbeat-interval: 30000

notifications:
  broadcast:
    enabled: true
    batch-size: 100
    flush-interval: 1000
```

### API Gateway Configuration

**File**: `microservices/api-gateway/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      routes:
        - id: eta-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/eta/**
        - id: routing-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/routes/**
        - id: returns-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/returns/**
        - id: notification-service
          uri: http://localhost:8084
          predicates:
            - Path=/api/v1/notifications/**,/ws/**
```

## 🎯 Configuration Best Practices

### 1. Environment Variables

Use environment variables for sensitive data:

```yaml
# application.yml
app:
  external:
    traffic:
      api-key: ${TRAFFIC_API_KEY:default-key}
      base-url: ${TRAFFIC_API_URL:https://api.traffic-service.com/v1}

# Set environment variables
export TRAFFIC_API_KEY=your-actual-api-key
export TRAFFIC_API_URL=https://api.traffic-service.com/v1
```

### 2. Profile-Specific Overrides

Override common configurations per environment:

```yaml
# application.yml (common)
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics

# application-prod.yml (production override)
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://mongodb:27017/logistics}
```

### 3. Configuration Validation

Add validation to configuration classes:

```java
@ConfigurationProperties(prefix = "eta")
@Validated
public class EtaProperties {
    
    @NotNull
    @Min(1)
    private Integer maxPredictionHours;
    
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double trafficWeight;
    
    // getters and setters
}
```

### 4. Conditional Configuration

Use conditional beans for environment-specific features:

```java
@Configuration
public class FeatureConfig {
    
    @Bean
    @ConditionalOnProperty(name = "app.features.eta-calculation.cache-enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.features.eta-calculation.mock-external-apis", havingValue = "true")
    public TrafficDataService mockTrafficDataService() {
        return new MockTrafficDataService();
    }
}
```

## 🔐 Security Configuration

### 1. Database Security

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://${MONGO_USERNAME:admin}:${MONGO_PASSWORD:password}@${MONGO_HOST:localhost}:${MONGO_PORT:27017}/${MONGO_DATABASE:logistics}?authSource=admin
    redis:
      password: ${REDIS_PASSWORD:}
      ssl: true
```

### 2. API Security

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    expiration: ${JWT_EXPIRATION:86400}
  
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
```

### 3. External API Security

```yaml
app:
  external:
    traffic:
      api-key: ${TRAFFIC_API_KEY}
      base-url: ${TRAFFIC_API_URL}
      timeout: 5000
      retry-attempts: 3
      retry-delay: 1000
```

## 📊 Monitoring Configuration

### 1. Prometheus Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

### 2. Logging Configuration

```yaml
logging:
  level:
    com.dpd.uk: INFO
    org.springframework.cloud: INFO
    org.springframework.web: WARN
  file:
    name: /var/log/logistics-platform/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

### 3. Tracing Configuration

```yaml
app:
  monitoring:
    jaeger:
      enabled: true
      endpoint: ${JAEGER_ENDPOINT:http://jaeger:14268/api/traces}
      service-name: ${JAEGER_SERVICE_NAME:logistics-platform}
```

## 🚀 Deployment Configuration

### 1. Docker Configuration

```yaml
# docker-compose.yml
version: '3.8'
services:
  eta-service:
    image: logistics-eta-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MONGODB_URI=mongodb://mongodb:27017/logistics
      - REDIS_HOST=redis
      - KAFKA_BROKERS=kafka:9092
```

### 2. Kubernetes Configuration

```yaml
# k8s/eta-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eta-service
spec:
  template:
    spec:
      containers:
      - name: eta-service
        image: logistics-eta-service:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: logistics-secret
              key: mongodb-uri
```

### 3. Helm Configuration

```yaml
# helm/logistics-platform/values.yaml
etaService:
  replicaCount: 3
  image:
    repository: logistics-eta-service
    tag: latest
  resources:
    limits:
      cpu: 500m
      memory: 1Gi
    requests:
      cpu: 250m
      memory: 512Mi
```

## 🔧 Configuration Management

### 1. Configuration Validation

```bash
# Validate configuration
mvn spring-boot:run -Dspring.profiles.active=dev --validate-config

# Check configuration properties
curl http://localhost:8080/actuator/configprops
```

### 2. Configuration Refresh

```bash
# Refresh configuration without restart
curl -X POST http://localhost:8080/actuator/refresh

# Check configuration changes
curl http://localhost:8080/actuator/env
```

### 3. Configuration Monitoring

```bash
# Monitor configuration changes
curl http://localhost:8080/actuator/configprops | jq '.contexts.application.beans'

# Check environment variables
curl http://localhost:8080/actuator/env | jq '.propertySources'
```

## 🐛 Troubleshooting Configuration

### 1. Common Issues

**Problem**: Configuration not loading
**Solution**: Check profile activation and file location

```bash
# Check active profiles
curl http://localhost:8080/actuator/env | jq '.activeProfiles'

# Check configuration sources
curl http://localhost:8080/actuator/configprops | jq '.contexts.application.beans'
```

**Problem**: Environment variables not resolved
**Solution**: Check variable names and values

```bash
# Check environment variables
env | grep -E "(SPRING_|APP_)"

# Test variable resolution
echo $TRAFFIC_API_KEY
```

**Problem**: Database connection issues
**Solution**: Verify connection strings and credentials

```bash
# Test MongoDB connection
mongosh "mongodb://localhost:27017/logistics"

# Test Redis connection
redis-cli ping
```

### 2. Configuration Debugging

```yaml
# Enable configuration debugging
logging:
  level:
    org.springframework.boot.context.config: DEBUG
    org.springframework.boot.autoconfigure: DEBUG
    org.springframework.cloud: DEBUG
```

### 3. Configuration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "eta.calculation.default-confidence=HIGH",
    "eta.calculation.cache-ttl-minutes=30"
})
class EtaConfigurationTest {
    
    @Test
    void shouldLoadConfiguration() {
        // Test configuration loading
    }
}
```

## 📚 Additional Resources

- [Spring Boot Configuration Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Spring Cloud Configuration](https://spring.io/projects/spring-cloud-config)
- [Docker Compose Configuration](https://docs.docker.com/compose/compose-file/)
- [Kubernetes Configuration](https://kubernetes.io/docs/concepts/configuration/)
- [Helm Configuration](https://helm.sh/docs/chart_template_guide/)

---

This configuration guide provides comprehensive information about managing configurations across different environments and modules in the Logistics Intelligence Platform. For additional support, please refer to the main project documentation or contact the development team.
