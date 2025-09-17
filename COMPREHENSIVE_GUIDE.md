# 🚚 Logistics Intelligence Platform - Comprehensive Guide

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Quick Start](#quick-start)
5. [Detailed Setup](#detailed-setup)
6. [API Documentation](#api-documentation)
7. [Frontend Usage](#frontend-usage)
8. [Monitoring & Observability](#monitoring--observability)
9. [Deployment](#deployment)
10. [Troubleshooting](#troubleshooting)
11. [Development Guide](#development-guide)

## 🎯 Overview

The Logistics Intelligence Platform is a modern, microservices-based system designed for DPD UK operations. It provides:

- **Predictive ETA** with real-time telematics and traffic data
- **Route Optimization** using multiple algorithms (Genetic, Simulated Annealing, Tabu Search)
- **Returns Orchestration** with state machine-based workflows
- **Real-time Operations Console** with live tracking and monitoring
- **Event-driven Architecture** using Apache Kafka
- **High Availability** with Kubernetes deployment

## 🏗️ Architecture

### Microservices

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   ETA Service   │    │ Routing Service │
│   (Port 8080)   │    │   (Port 8081)   │    │   (Port 8082)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
         │ Returns Service │    │Notification Svc │    │   Ops Console   │
         │   (Port 8083)   │    │   (Port 8084)   │    │   (Port 3000)   │
         └─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Data Flow

```
Parcel Creation → ETA Calculation → Route Optimization → Real-time Tracking
       ↓                ↓                    ↓                    ↓
   Kafka Events → State Management → WebSocket Updates → Dashboard Display
```

## 🔧 Prerequisites

### Required Software

- **Java 21+**
- **Maven 3.8+**
- **Node.js 18+**
- **Docker & Docker Compose**
- **Kubernetes** (for production)
- **Git**

### External Services

- **MongoDB** (or Cosmos DB)
- **Redis**
- **Apache Kafka**
- **Mapping API** (Azure Maps/Google Maps)
- **Traffic API**

## 🚀 Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd DPD-UK
```

### 2. Start Infrastructure

```bash
# Start all required services
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 3. Build and Run Services

```bash
# Build all microservices
mvn clean install

# Run services in separate terminals
mvn spring-boot:run -pl microservices/eta-service
mvn spring-boot:run -pl microservices/routing-service
mvn spring-boot:run -pl microservices/returns-service
mvn spring-boot:run -pl microservices/notification-service
mvn spring-boot:run -pl microservices/api-gateway
```

### 4. Start Frontend

```bash
cd frontend/ops-console
npm install
npm run dev
```

### 5. Access the Platform

- **API Gateway**: http://localhost:8080
- **Ops Console**: http://localhost:3000
- **Kafka UI**: http://localhost:8080 (port 8080)
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jaeger**: http://localhost:16686

## 📚 Detailed Setup

### Environment Configuration

#### 1. Database Setup

```yaml
# application.yml for each service
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/logistics_eta
  redis:
    host: localhost
    port: 6379
    database: 0
```

#### 2. Kafka Configuration

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
          auto-create-topics: true
```

#### 3. External API Keys

```bash
export TRAFFIC_API_KEY="your-traffic-api-key"
export MAPPING_API_KEY="your-mapping-api-key"
```

### Service Configuration

#### ETA Service

```yaml
# microservices/eta-service/src/main/resources/application.yml
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

#### Routing Service

```yaml
# microservices/routing-service/src/main/resources/application.yml
routing:
  optimization:
    algorithm: GENETIC_ALGORITHM
    max-iterations: 1000
    population-size: 50
  constraints:
    max-route-duration-hours: 8
    max-route-distance-km: 200
```

#### Returns Service

```yaml
# microservices/returns-service/src/main/resources/application.yml
returns:
  ttl:
    default-hours: 24
    max-hours: 72
  approval:
    auto-approve-threshold: 50.0
    manual-review-threshold: 100.0
```

## 📖 API Documentation

### ETA Service API

#### Calculate ETA

```http
POST /api/v1/eta/calculate
Content-Type: application/json

{
  "parcelId": "PARCEL_123",
  "trackingNumber": "TRK123456789",
  "origin": {
    "line1": "123 Main Street",
    "city": "London",
    "postcode": "SW1A 1AA",
    "country": "UK",
    "latitude": 51.5074,
    "longitude": -0.1278
  },
  "destination": {
    "line1": "456 High Street",
    "city": "Manchester",
    "postcode": "M1 1AA",
    "country": "UK",
    "latitude": 53.4808,
    "longitude": -2.2426
  },
  "type": "STANDARD",
  "weight": 2.5,
  "depotId": "LONDON_DEPOT",
  "driverId": "DRV_001",
  "vehicleId": "VAN_001"
}
```

**Response:**
```json
{
  "parcelId": "PARCEL_123",
  "estimatedArrival": "2024-01-15T14:30:00",
  "confidence": "HIGH",
  "estimatedMinutes": 120,
  "distanceKm": 45.2,
  "factors": {
    "traffic": {"multiplier": 1.1, "congestionLevel": "MODERATE"},
    "historical": {"multiplier": 0.95, "sampleSize": 25},
    "depot": {"multiplier": 1.0, "capacityUtilization": 0.7},
    "vehicle": {"multiplier": 1.0, "reliability": 0.95}
  }
}
```

#### Get ETA

```http
GET /api/v1/eta/{parcelId}
```

#### Update ETA

```http
PUT /api/v1/eta/{parcelId}/update
Content-Type: application/json

{
  "traffic": {"multiplier": 1.2, "incidents": 3},
  "vehicle": {"location": "IN_TRANSIT", "batteryLevel": 80}
}
```

### Routing Service API

#### Optimize Route

```http
POST /api/v1/routes/optimize
Content-Type: application/json

{
  "depotId": "LONDON_DEPOT",
  "driverId": "DRV_001",
  "vehicleId": "VAN_001",
  "stops": [
    {
      "line1": "123 Main Street",
      "city": "London",
      "postcode": "SW1A 1AA",
      "latitude": 51.5074,
      "longitude": -0.1278
    },
    {
      "line1": "456 High Street",
      "city": "Manchester",
      "postcode": "M1 1AA",
      "latitude": 53.4808,
      "longitude": -2.2426
    }
  ],
  "algorithm": "GENETIC_ALGORITHM",
  "maxDurationHours": 8,
  "maxDistanceKm": 200
}
```

**Response:**
```json
{
  "optimizedSequence": [0, 1],
  "totalDistance": 45.2,
  "totalTime": 120,
  "optimizationTime": 1500,
  "iterations": 500,
  "fitness": 0.85,
  "algorithm": "GENETIC_ALGORITHM",
  "status": "SUCCESS"
}
```

#### Get Route

```http
GET /api/v1/routes/{routeId}
```

#### Update Route Status

```http
PUT /api/v1/routes/{routeId}/status?status=IN_PROGRESS
```

#### Reroute

```http
POST /api/v1/routes/{routeId}/reroute
Content-Type: application/json

[
  {
    "line1": "789 New Street",
    "city": "Birmingham",
    "postcode": "B1 1AA",
    "latitude": 52.4862,
    "longitude": -1.8904
  }
]
```

### Returns Service API

#### Initiate Return

```http
POST /api/v1/returns/initiate
Content-Type: application/json

{
  "parcelId": "PARCEL_123",
  "trackingNumber": "TRK123456789",
  "customerId": "CUST_001",
  "reason": "DAMAGED_GOODS",
  "description": "Package arrived damaged",
  "pickupAddress": {
    "line1": "123 Main Street",
    "city": "London",
    "postcode": "SW1A 1AA",
    "latitude": 51.5074,
    "longitude": -0.1278
  },
  "returnAddress": {
    "line1": "456 Depot Road",
    "city": "London",
    "postcode": "E1 1AA",
    "latitude": 51.5074,
    "longitude": -0.1278
  },
  "type": "CUSTOMER_INITIATED",
  "priority": "MEDIUM",
  "refundAmount": 25.99
}
```

#### Approve Return

```http
POST /api/v1/returns/{returnId}/approve?approvalCode=APP_123
```

#### Schedule Pickup

```http
POST /api/v1/returns/{returnId}/schedule-pickup?driverId=DRV_001&vehicleId=VAN_001
```

#### Complete Pickup

```http
POST /api/v1/returns/{returnId}/complete-pickup
```

#### Complete Processing

```http
POST /api/v1/returns/{returnId}/complete-processing
```

## 🖥️ Frontend Usage

### Operations Console Features

#### 1. Dashboard Overview

- **Real-time Metrics**: Live ETA accuracy, route efficiency, return rates
- **Active Deliveries**: Current deliveries with status and location
- **Driver Status**: Real-time driver locations and availability
- **System Health**: Service status and performance metrics

#### 2. ETA Management

- **Parcel Tracking**: Search and track individual parcels
- **ETA Updates**: Real-time ETA adjustments and notifications
- **Confidence Levels**: Visual indicators for ETA reliability
- **Historical Analysis**: ETA accuracy trends and improvements

#### 3. Route Optimization

- **Route Planning**: Create and optimize delivery routes
- **Algorithm Selection**: Choose optimization algorithm
- **Real-time Rerouting**: Dynamic route adjustments
- **Performance Metrics**: Route efficiency and cost analysis

#### 4. Returns Management

- **Return Requests**: View and manage return requests
- **Approval Workflow**: Approve or reject returns
- **Pickup Scheduling**: Assign drivers and vehicles
- **Processing Status**: Track return processing stages

#### 5. Live Tracking

- **Map View**: Interactive map with real-time locations
- **Driver Tracking**: Live driver positions and routes
- **Parcel Status**: Real-time parcel status updates
- **Traffic Information**: Live traffic conditions

### WebSocket Integration

```javascript
// Connect to WebSocket
const socket = new WebSocket('ws://localhost:8084/ws/notifications');

// Subscribe to notifications
socket.send(JSON.stringify({
  type: 'subscribe',
  topic: 'eta-updates'
}));

// Handle notifications
socket.onmessage = (event) => {
  const notification = JSON.parse(event.data);
  console.log('Received notification:', notification);
};
```

## 📊 Monitoring & Observability

### Metrics

#### Prometheus Metrics

- **ETA Accuracy**: Percentage of accurate ETA predictions
- **Route Efficiency**: Average route optimization improvement
- **Return Processing Time**: Average time to process returns
- **System Performance**: Response times and throughput

#### Custom Metrics

```java
// ETA calculation metrics
@Timed(name = "eta.calculation.time")
@Counted(name = "eta.calculation.count")
public ETA calculateETA(Parcel parcel) {
    // Implementation
}

// Route optimization metrics
@Gauge(name = "route.optimization.fitness")
public double getOptimizationFitness() {
    // Implementation
}
```

### Logging

#### Structured Logging

```java
log.info("Calculating ETA for parcel: {} with confidence: {}", 
    parcelId, confidence);
log.error("Failed to calculate ETA for parcel: {}", parcelId, exception);
```

#### Log Levels

- **DEBUG**: Detailed flow information
- **INFO**: Important business events
- **WARN**: Potential issues
- **ERROR**: Error conditions

### Tracing

#### OpenTelemetry Integration

```java
@Span("eta-calculation")
public ETA calculateETA(Parcel parcel) {
    Span.current().setAttribute("parcel.id", parcel.getParcelId());
    Span.current().setAttribute("parcel.type", parcel.getType().name());
    // Implementation
}
```

### Health Checks

#### Service Health

```http
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "mongo": {"status": "UP"},
    "redis": {"status": "UP"},
    "kafka": {"status": "UP"}
  }
}
```

## 🚀 Deployment

### Docker Deployment

#### 1. Build Images

```bash
# Build ETA service
docker build -t logistics-eta-service ./microservices/eta-service

# Build Routing service
docker build -t logistics-routing-service ./microservices/routing-service

# Build Returns service
docker build -t logistics-returns-service ./microservices/returns-service

# Build Notification service
docker build -t logistics-notification-service ./microservices/notification-service

# Build API Gateway
docker build -t logistics-api-gateway ./microservices/api-gateway
```

#### 2. Run with Docker Compose

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment

#### 1. Create Namespace

```bash
kubectl create namespace logistics-platform
```

#### 2. Deploy Services

```bash
# Deploy ETA service
kubectl apply -f k8s/eta-service/

# Deploy Routing service
kubectl apply -f k8s/routing-service/

# Deploy Returns service
kubectl apply -f k8s/returns-service/

# Deploy Notification service
kubectl apply -f k8s/notification-service/

# Deploy API Gateway
kubectl apply -f k8s/api-gateway/
```

#### 3. Deploy Frontend

```bash
# Build and deploy frontend
cd frontend/ops-console
npm run build
kubectl apply -f k8s/ops-console/
```

### Helm Charts

#### 1. Install Dependencies

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

#### 2. Install Infrastructure

```bash
# Install MongoDB
helm install mongodb bitnami/mongodb -n logistics-platform

# Install Redis
helm install redis bitnami/redis -n logistics-platform

# Install Kafka
helm install kafka bitnami/kafka -n logistics-platform
```

#### 3. Deploy Application

```bash
# Deploy with Helm
helm install logistics-platform ./helm/logistics-platform -n logistics-platform
```

## 🔧 Troubleshooting

### Common Issues

#### 1. Service Not Starting

**Problem**: Service fails to start
**Solution**: Check logs and dependencies

```bash
# Check service logs
kubectl logs -f deployment/eta-service -n logistics-platform

# Check service status
kubectl get pods -n logistics-platform
```

#### 2. Database Connection Issues

**Problem**: Cannot connect to MongoDB/Redis
**Solution**: Verify connection strings and network

```bash
# Test MongoDB connection
kubectl exec -it deployment/mongodb -- mongosh

# Test Redis connection
kubectl exec -it deployment/redis -- redis-cli ping
```

#### 3. Kafka Connection Issues

**Problem**: Kafka topics not created
**Solution**: Check Kafka configuration

```bash
# List Kafka topics
kubectl exec -it deployment/kafka -- kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create topic manually
kubectl exec -it deployment/kafka -- kafka-topics.sh --bootstrap-server localhost:9092 --create --topic eta-calculated
```

#### 4. Frontend Not Loading

**Problem**: Ops console not accessible
**Solution**: Check API Gateway and CORS

```bash
# Check API Gateway logs
kubectl logs -f deployment/api-gateway -n logistics-platform

# Test API Gateway
curl http://localhost:8080/api/v1/eta/health
```

### Performance Issues

#### 1. Slow ETA Calculations

**Solution**: Optimize caching and database queries

```yaml
# Increase cache TTL
eta:
  calculation:
    cache-ttl-minutes: 30
```

#### 2. High Memory Usage

**Solution**: Adjust JVM settings

```yaml
# JVM configuration
env:
  - name: JAVA_OPTS
    value: "-Xms512m -Xmx2g -XX:+UseG1GC"
```

#### 3. Database Performance

**Solution**: Add indexes and optimize queries

```javascript
// MongoDB indexes
db.eta_calculations.createIndex({ "parcelId": 1, "isActive": 1 })
db.return_requests.createIndex({ "status": 1, "isActive": 1 })
```

## 👨‍💻 Development Guide

### Project Structure

```
DPD-UK/
├── microservices/
│   ├── eta-service/
│   ├── routing-service/
│   ├── returns-service/
│   ├── notification-service/
│   └── api-gateway/
├── frontend/
│   └── ops-console/
├── shared/
│   └── common-lib/
├── k8s/
├── helm/
├── monitoring/
└── docs/
```

### Adding New Features

#### 1. New Microservice

```bash
# Create new service
mkdir microservices/new-service
cd microservices/new-service

# Create pom.xml
# Create application.yml
# Create main application class
# Create controllers, services, repositories
```

#### 2. New API Endpoint

```java
@RestController
@RequestMapping("/api/v1/new-feature")
public class NewFeatureController {
    
    @PostMapping
    public ResponseEntity<Response> create(@RequestBody Request request) {
        // Implementation
    }
}
```

#### 3. Frontend Component

```vue
<template>
  <div class="new-component">
    <!-- Component template -->
  </div>
</template>

<script setup lang="ts">
// Component logic
</script>
```

### Testing

#### Unit Tests

```java
@SpringBootTest
class EtaCalculationServiceTest {
    
    @Test
    void shouldCalculateETA() {
        // Test implementation
    }
}
```

#### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EtaControllerIntegrationTest {
    
    @Test
    void shouldReturnETA() {
        // Test implementation
    }
}
```

#### End-to-End Tests

```javascript
describe('ETA Calculation', () => {
  it('should calculate ETA for parcel', async () => {
    // Test implementation
  });
});
```

### Code Quality

#### Linting

```bash
# Backend
mvn checkstyle:check

# Frontend
npm run lint
```

#### Testing

```bash
# Run all tests
mvn test
npm run test

# Run specific test
mvn test -Dtest=EtaCalculationServiceTest
```

### Git Workflow

#### Branching Strategy

- **main**: Production-ready code
- **develop**: Integration branch
- **feature/***: Feature development
- **hotfix/***: Critical fixes

#### Commit Messages

```
feat: add ETA calculation service
fix: resolve routing optimization issue
docs: update API documentation
test: add unit tests for ETA service
```

## 📈 Performance Optimization

### Database Optimization

#### MongoDB

```javascript
// Create compound indexes
db.eta_calculations.createIndex({ 
  "parcelId": 1, 
  "isActive": 1, 
  "calculatedAt": -1 
})

// Use projection to limit fields
db.eta_calculations.find(
  { "parcelId": "PARCEL_123" },
  { "estimatedArrival": 1, "confidence": 1 }
)
```

#### Redis

```yaml
# Redis configuration
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

### Caching Strategy

#### Application Level

```java
@Cacheable(value = "eta-calculations", key = "#parcelId")
public ETA getETA(String parcelId) {
    // Implementation
}
```

#### HTTP Level

```yaml
# API Gateway caching
spring:
  cloud:
    gateway:
      default-filters:
        - name: AddResponseHeader
          args:
            name: Cache-Control
            value: "max-age=300"
```

### Load Balancing

#### Kubernetes Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: eta-service
spec:
  selector:
    app: eta-service
  ports:
    - port: 8081
      targetPort: 8081
  type: LoadBalancer
```

## 🔒 Security

### Authentication

#### JWT Configuration

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    expiration: 86400
```

#### API Key Authentication

```java
@Component
public class ApiKeyAuthenticationFilter implements WebFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Implementation
    }
}
```

### Data Protection

#### Encryption

```java
@Configuration
public class EncryptionConfig {
    
    @Bean
    public StringEncryptor stringEncryptor() {
        // Implementation
    }
}
```

#### PII Handling

```java
@JsonIgnore
private String customerEmail;

@JsonProperty("email")
public String getMaskedEmail() {
    return maskEmail(customerEmail);
}
```

## 📚 Additional Resources

### Documentation

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Vue.js Documentation](https://vuejs.org/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

### Monitoring Tools

- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Jaeger](https://www.jaegertracing.io/)
- [ELK Stack](https://www.elastic.co/elastic-stack/)

### Best Practices

- [Microservices Patterns](https://microservices.io/)
- [12-Factor App](https://12factor.net/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

## 🆘 Support

### Getting Help

1. **Check Logs**: Always check service logs first
2. **Health Checks**: Verify service health endpoints
3. **Documentation**: Consult this guide and API docs
4. **Community**: Join our developer community
5. **Support**: Contact the development team

### Contact Information

- **Email**: dev-team@dpd-uk.com
- **Slack**: #logistics-platform
- **GitHub**: [Repository Issues](https://github.com/dpd-uk/logistics-platform/issues)

---

**Happy Coding! 🚀**

This comprehensive guide should help you understand, deploy, and maintain the Logistics Intelligence Platform. For additional support or questions, please refer to the contact information above.
