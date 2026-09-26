# ---------- build: compila o jar dentro do próprio serviço de hospedagem ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- runtime: só o JRE, memória limitada para caber em planos gratuitos (512 MB) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/overall-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xmx300m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
