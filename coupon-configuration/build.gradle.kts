import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	java
	id("org.springframework.boot")
}

dependencies {
	implementation(project(":coupon-domain"))
	implementation(project(":coupon-application"))
	implementation(project(":coupon-adapter-in-rest"))
	implementation(project(":coupon-adapter-out-persistence"))
	implementation(project(":coupon-adapter-out-geolocation"))

	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
	implementation("org.flywaydb:flyway-database-postgresql")
	runtimeOnly("org.postgresql:postgresql")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
}

tasks.named<BootRun>("bootRun") {
	workingDir = rootProject.projectDir
}
