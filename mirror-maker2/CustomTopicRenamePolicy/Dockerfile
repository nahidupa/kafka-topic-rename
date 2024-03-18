FROM maven:3.8.7-openjdk-18 as builder
WORKDIR /app
COPY . .
RUN mvn clean install

# FROM quay.io/strimzi/kafka:latest-kafka-3.4.1
FROM quay.io/strimzi/kafka:latest-kafka-3.7.0
USER root:root
COPY --from=builder /app/target/CustomReplicationPolicy-1.0-SNAPSHOT.jar /opt/kafka/libs/
USER 1001
