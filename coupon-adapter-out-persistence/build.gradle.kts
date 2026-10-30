plugins {
	java
}

dependencies {
	implementation(project(":coupon-application"))
	implementation(project(":coupon-domain"))
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
