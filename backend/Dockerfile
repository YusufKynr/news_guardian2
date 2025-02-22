# OpenJDK 17 kullan
FROM openjdk:17
WORKDIR /app

# Maven Wrapper dosyalarını kopyala
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
COPY src src

# Maven Wrapper'a çalıştırma izni ver
RUN chmod +x mvnw

# Maven ile build et
RUN ./mvnw package -DskipTests

# Spring Boot uygulamasını çalıştır
CMD ["java", "-jar", "target/*.jar"]
