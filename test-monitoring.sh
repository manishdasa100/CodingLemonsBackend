#!/bin/bash

echo "Testing CodingLemons Backend Monitoring Setup"
echo "==========================================="

# Start the application in background
echo "Starting application..."
cd app
./gradlew bootRun &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 30

# Test health endpoint
echo "Testing health endpoint..."
curl -s http://localhost:3000/actuator/health | jq .

echo ""

# Test metrics endpoint
echo "Testing metrics endpoint..."
curl -s http://localhost:3000/actuator/metrics | jq .

echo ""

# Test prometheus endpoint
echo "Testing prometheus metrics endpoint..."
curl -s http://localhost:3000/actuator/prometheus | head -20

echo ""

# Test info endpoint
echo "Testing info endpoint..."
curl -s http://localhost:3000/actuator/info | jq .

echo ""
echo "Monitoring setup test completed!"
echo "Kill the application process with: kill $APP_PID"