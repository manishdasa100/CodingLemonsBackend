# CodingLemons Backend - Monitoring & Logging Setup

This document describes the comprehensive monitoring and logging solution implemented for the CodingLemons Backend application.

## Overview

The application now includes production-ready logging and monitoring capabilities:

- **Structured JSON logging** with correlation IDs
- **Comprehensive metrics** collection with Prometheus
- **Health checks** for all external dependencies
- **Distributed tracing** support
- **Log aggregation** with ELK stack
- **Real-time monitoring dashboards** with Grafana

## Architecture

### Logging Stack
- **Logback**: Structured JSON logging with async appenders
- **SLF4J**: Logging abstraction layer
- **MDC**: Request correlation and context tracking
- **Elasticsearch**: Log storage and indexing
- **Kibana**: Log visualization and search
- **Filebeat**: Log shipping and parsing

### Monitoring Stack
- **Micrometer**: Metrics collection framework
- **Prometheus**: Metrics storage and alerting
- **Grafana**: Visualization and dashboards
- **Spring Boot Actuator**: Application health and metrics endpoints

## Features Implemented

### 1. Structured Logging
- JSON format logs in production
- Human-readable logs in development
- Request correlation IDs
- Contextual information (user IP, user agent, etc.)
- Async logging for better performance
- Log rotation and compression
- Centralized error logging with stack traces

### 2. Application Metrics
- HTTP request/response metrics
- JVM metrics (memory, GC, threads)
- Custom business metrics:
  - Problem submission counts
  - User registration counts
  - Code execution timing
  - API request counts
- Database connection pool metrics
- Cache hit/miss ratios

### 3. Health Checks
- Application health endpoint
- Custom health indicators for:
  - MongoDB connectivity
  - Redis connectivity
  - RabbitMQ connectivity
  - Memory usage monitoring
- Detailed health information

### 4. Request/Response Logging
- Automatic request logging with correlation IDs
- Response time tracking
- Error tracking and categorization
- Client IP and User-Agent logging
- Request size and payload logging (configurable)

### 5. AOP-based Method Monitoring
- Automatic timing of controller and service methods
- Exception tracking with method context
- Call count metrics
- Performance bottleneck identification

## Configuration

### Application Properties

Key monitoring configurations in `application.properties`:

```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus,loggers,env,configprops,scheduledtasks,mappings,httptrace
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true

# Logging
logging.level.root=INFO
logging.level.com.codinglemonsbackend=INFO
```

### Environment-Specific Logging

- **Development**: Human-readable console logs
- **Production**: Structured JSON logs with full context
- **Test**: Minimal logging for faster test execution

## Monitoring Stack Deployment

### Prerequisites
- Docker and Docker Compose
- 8GB RAM minimum for full stack
- Available ports: 3001 (Grafana), 9090 (Prometheus), 5601 (Kibana), 9200 (Elasticsearch)

### Starting the Monitoring Stack

```bash
# Start the monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d

# Start your application
./gradlew bootRun
```

### Accessing Services

- **Application Health**: http://localhost:3000/actuator/health
- **Application Metrics**: http://localhost:3000/actuator/metrics
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin123)
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200

## Key Metrics to Monitor

### Application Performance
- `http_server_requests_seconds` - Request latency
- `http_server_requests_seconds_count` - Request rate
- `method_execution_time` - Method performance
- `method_calls_total` - Method call frequency

### Application Health
- `application_errors_total` - Error rates by type
- `jvm_memory_used_bytes` - Memory usage
- `jvm_gc_pause_seconds` - Garbage collection
- `hikaricp_connections` - Database connection pool

### Business Metrics
- `problem_submissions_total` - User engagement
- `user_registrations_total` - Growth metrics
- `problem_execution_duration` - Code execution performance

## Alerting (Future Enhancement)

Recommended alerts to set up:

1. **High Error Rate**: > 5% of requests failing
2. **High Response Time**: 95th percentile > 2 seconds  
3. **Low Memory**: < 10% heap memory available
4. **Database Connection Issues**: MongoDB/Redis health checks failing
5. **High CPU Usage**: > 80% for extended periods

## Log Analysis

### Common Log Queries (Kibana)

1. **Find all errors for a specific request**:
   ```
   requestId: "abc123" AND level: "ERROR"
   ```

2. **Monitor API endpoint performance**:
   ```
   uri: "/api/problems/*" AND message: "Controller method*"
   ```

3. **Track user activity**:
   ```
   clientIp: "192.168.1.100" AND level: "INFO"
   ```

## Troubleshooting

### Common Issues

1. **Logs not appearing in Kibana**
   - Check Filebeat is running and has access to log files
   - Verify Elasticsearch is healthy
   - Check log file permissions

2. **Metrics not showing in Grafana**
   - Verify Prometheus is scraping the application
   - Check Spring Boot Actuator endpoints are accessible
   - Confirm application is exposing metrics

3. **High memory usage**
   - Monitor JVM heap usage metrics
   - Check for memory leaks in custom code
   - Adjust JVM heap size if needed

### Performance Tuning

1. **Reduce log volume**: Adjust logging levels in production
2. **Optimize metrics collection**: Disable unnecessary metrics
3. **Configure log retention**: Set appropriate retention policies
4. **Use async logging**: Already configured for better performance

## Security Considerations

1. **Log sanitization**: Sensitive data is not logged
2. **Access control**: Monitoring endpoints should be secured in production
3. **Network security**: Use proper firewall rules for monitoring stack
4. **Data retention**: Configure appropriate log retention policies

## Maintenance

### Regular Tasks
1. **Monitor disk space**: Logs and metrics consume storage
2. **Update dashboards**: Keep Grafana dashboards current
3. **Review alerts**: Adjust thresholds based on actual usage
4. **Clean old logs**: Ensure log rotation is working
5. **Security updates**: Keep monitoring stack components updated

This monitoring setup provides comprehensive observability for the CodingLemons Backend, enabling proactive monitoring, quick issue resolution, and performance optimization.