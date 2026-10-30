plugins {
	java
}

val resilience4jVersion = "2.3.0"

dependencies {
	implementation(project(":coupon-application"))
	implementation(project(":coupon-domain"))
	implementation("org.springframework:spring-web")
	implementation("org.springframework.boot:spring-boot-starter-jackson")
	implementation("com.github.ben-manes.caffeine:caffeine")
	implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
	implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
	implementation("io.github.resilience4j:resilience4j-timelimiter:$resilience4jVersion")

	testImplementation("org.wiremock:wiremock-standalone:3.13.1")
}
