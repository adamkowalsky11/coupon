plugins {
	java
}

dependencies {
	implementation(project(":coupon-application"))
	implementation(project(":coupon-domain"))
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework:spring-tx")
	implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.55")
}
