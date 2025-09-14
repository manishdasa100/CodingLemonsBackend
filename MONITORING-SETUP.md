# CodingLemons Backend - Complete Monitoring Setup Guide

This guide provides step-by-step instructions for setting up comprehensive monitoring and metrics collection for the CodingLemons Backend application.

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Java 17+ for running the Spring Boot application
- At least 8GB RAM for running the full monitoring stack

### 1. Start the Monitoring Stack
```bash
# Start all monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Verify all services are running
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. Start the Application
```bash
# Build and run the application
./gradlew bootRun

# Or with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 3. Access Monitoring Services
- **Application**: http://localhost:3000
- **Grafana Dashboards**: http://localhost:3001 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **Kibana Logs**: http://localhost:5601
- **AlertManager**: http://localhost:9093
- **Container Metrics**: http://localhost:8080 (cAdvisor)
- **System Metrics**: http://localhost:9100 (Node Exporter)

## 📊 Metrics Collected

### User Engagement Metrics
- **User Registration Rate**: `user_registrations_total`
- **User Login Rate**: `user_logins_total`
- **Daily Active Users**: `user_active_daily`
- **Weekly Active Users**: `user_active_weekly`
- **Monthly Active Users**: `user_active_monthly`
- **Code Execution Volume**: `code_executions_total`
- **Problem Submissions**: `problem_submissions_total`

### Application Performance Metrics
- **API Response Times**: `api_response_duration_seconds`
- **Request Rate**: `api_requests_total`
- **Error Rate**: `api_errors_total` and `api_exceptions_total`
- **API Throughput**: `api_throughput`
- **Method Execution Times**: `method_execution_time_seconds`

### Database Performance Metrics
- **Read Latency**: `database_read_duration_seconds`
- **Write Latency**: `database_write_duration_seconds`
- **Connection Pool Metrics**: `hikaricp_connections`

### S3 Performance Metrics
- **Upload Performance**: `s3_upload_duration_seconds`
- **Upload Size**: `s3_upload_size_bytes`
- **S3 Operations**: `s3_uploads_total`, `s3_downloads_total`, `s3_deletes_total`
- **Error Rates**: `s3_upload_errors_total`, `s3_download_errors_total`

### RabbitMQ Performance Metrics
- **Message Processing Time**: `rabbitmq_message_processing_duration_seconds`
- **Message Throughput**: `rabbitmq_messages_published_total`, `rabbitmq_messages_consumed_total`
- **Queue Size**: `rabbitmq_queue_size`

### Infrastructure Metrics
- **Health Status**: Application health endpoints
- **Network I/O**: `system_network_input_bytes`, `system_network_output_bytes`
- **Container Resources**: `container_cpu_usage_percent`, `container_memory_usage_bytes`
- **JVM Metrics**: Memory, GC, thread counts

## 📈 Grafana Dashboards

### Main Dashboard
Import the comprehensive dashboard from:
`monitoring/grafana/dashboards/codinglemons-comprehensive-dashboard.json`

The dashboard includes panels for:
1. **User Engagement**: Registration rates, active users, code execution volume
2. **Application Performance**: Response times, error rates, throughput
3. **Database Performance**: Read/write latency, connection metrics
4. **S3 Performance**: Upload times, file sizes, operation rates
5. **RabbitMQ Performance**: Message processing, queue sizes
6. **Infrastructure**: Health status, resource usage, JVM metrics

### Setting Up Dashboards
1. Access Grafana at http://localhost:3001
2. Login with admin/admin123
3. Go to Dashboards → Import
4. Upload the JSON file or paste the content
5. Configure data source as Prometheus (http://prometheus:9090)

## 🚨 Alerting Rules

### Alert Categories

#### Critical Alerts
- **Application Down**: `up{job="codinglemons-backend"} == 0`
- **High Error Rate**: `rate(api_errors_total[5m]) > 0.05`

#### Warning Alerts
- **High Response Time**: 95th percentile > 2 seconds
- **High Memory Usage**: JVM heap > 85%
- **High CPU Usage**: Container CPU > 80%
- **Slow Database Queries**: Read/Write operations > 1-2 seconds
- **High S3 Error Rate**: S3 errors > 0.1/sec
- **Large Message Queue**: RabbitMQ queue > 1000 messages

#### Info Alerts
- **Low User Registrations**: Registration rate < 0.001/hour
- **No Code Executions**: No executions for 30 minutes

### Alert Configuration
- **Prometheus**: Rules defined in `monitoring/prometheus/alert_rules.yml`
- **AlertManager**: Configuration in `monitoring/alertmanager/alertmanager.yml`
- **Notifications**: Email, Slack (configure webhooks)

## 🔧 Configuration Details

### Spring Boot Configuration
Key properties in `application.properties`:
```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

### Monitoring Services Configuration

#### Prometheus
- **Config**: `monitoring/prometheus/prometheus.yml`
- **Scrape Interval**: 5 seconds for application, 15 seconds for others
- **Retention**: 200 hours
- **Targets**: Application, Node Exporter, cAdvisor

#### Grafana
- **Datasource**: Prometheus (auto-configured)
- **Dashboards**: Auto-imported from `monitoring/grafana/dashboards/`
- **Plugins**: Pre-configured for comprehensive visualization

## 🐳 Docker Services

### Core Monitoring Stack
- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and dashboarding  
- **AlertManager**: Alert routing and notification

### Log Management
- **Elasticsearch**: Log storage and indexing
- **Kibana**: Log visualization and search
- **Filebeat**: Log shipping and parsing

### Infrastructure Monitoring
- **Node Exporter**: System metrics (CPU, memory, disk, network)
- **cAdvisor**: Container resource usage metrics

## 🔍 Troubleshooting

### Common Issues

1. **Metrics not showing in Grafana**
   - Verify Prometheus is scraping: http://localhost:9090/targets
   - Check application actuator endpoint: http://localhost:3000/actuator/prometheus
   - Ensure correct datasource configuration in Grafana

2. **High memory usage**
   - Monitor JVM metrics in Grafana
   - Adjust heap size: `-Xmx2g -Xms1g`
   - Check for memory leaks in custom code

3. **Logs not appearing in Kibana**
   - Verify Elasticsearch is healthy: http://localhost:9200/_cluster/health
   - Check Filebeat configuration and log file permissions
   - Ensure log rotation is working properly

4. **Alerts not firing**
   - Check Prometheus rules: http://localhost:9090/rules
   - Verify AlertManager configuration: http://localhost:9093
   - Test alert expressions in Prometheus query interface

### Performance Tuning

1. **Reduce log volume**: Adjust logging levels for production
2. **Optimize metrics collection**: Disable unnecessary metrics if needed
3. **Configure retention policies**: Set appropriate data retention periods
4. **Resource allocation**: Monitor container resource usage and adjust limits

## 📝 Maintenance Tasks

### Daily
- Monitor dashboard alerts and notifications
- Check application health status
- Review error rates and response times

### Weekly  
- Analyze user engagement trends
- Review capacity planning metrics
- Check log storage usage

### Monthly
- Update monitoring stack components
- Review and adjust alert thresholds
- Archive old logs and metrics
- Performance optimization review

## 🔒 Security Considerations

1. **Authentication**: Secure Grafana, Prometheus, and other monitoring UIs
2. **Network Security**: Use proper firewall rules and network segmentation
3. **Data Privacy**: Ensure no sensitive data is logged or exposed in metrics
4. **Access Control**: Implement role-based access for monitoring tools
5. **SSL/TLS**: Enable HTTPS for monitoring endpoints in production

## 📚 Additional Resources

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/)
- [Grafana Dashboard Creation](https://grafana.com/docs/grafana/latest/dashboards/)
- [Spring Boot Actuator Guide](https://spring.io/guides/gs/actuator-service/)

## 🤝 Support

For monitoring-related issues:
1. Check application logs first
2. Verify monitoring service health
3. Review Prometheus targets and metrics
4. Check Grafana datasource connectivity
5. Validate alert rule configurations

This comprehensive monitoring setup provides full observability into your CodingLemons Backend application, enabling proactive monitoring, quick issue resolution, and data-driven optimization decisions.