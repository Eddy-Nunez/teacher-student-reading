# ---- Stage 1: build the React SPA ----
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
# outputs /app/dist

# ---- Stage 2: build the Spring Boot fat jar, with the SPA baked into static ----
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline || true
COPY backend/src ./src
# Bake the built SPA into the backend's classpath so ONE process serves UI + API (single origin).
COPY --from=frontend /app/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package
# produces /app/target/portal-0.0.1-SNAPSHOT.jar

# ---- Stage 3: minimal JRE runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /app/target/portal-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080

# Render injects $PORT; bind to it (default 8080). Read the rest from env (relaxed binding).
ENTRYPOINT ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
