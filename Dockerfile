FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY . .

RUN chmod +x gradlew \
    && ./gradlew :coupon-configuration:bootJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --create-home --uid 1001 coupon

COPY --from=build /workspace/coupon-configuration/build/libs/coupon-configuration-0.0.1-SNAPSHOT.jar /app/app.jar

USER coupon

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
