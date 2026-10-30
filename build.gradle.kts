import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
	java
	id("org.springframework.boot") version "4.1.2" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
	group = "com.dev"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "java")
	apply(plugin = "io.spring.dependency-management")

	extensions.configure<JavaPluginExtension> {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(25))
		}
	}

	configure<DependencyManagementExtension> {
		imports {
			mavenBom(SpringBootPlugin.BOM_COORDINATES)
		}
	}

	dependencies {
		add("testImplementation", "org.junit.jupiter:junit-jupiter")
		add("testImplementation", "org.assertj:assertj-core")
		add("testImplementation", "org.mockito:mockito-core")
		add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
	}

	val mockitoAgent = configurations.create("mockitoAgent") {
		isCanBeConsumed = false
		isCanBeResolved = true
	}

	dependencies {
		add(mockitoAgent.name, "org.mockito:mockito-core") {
			isTransitive = false
		}
	}

	val forceTestExecution = gradle.startParameter.taskNames.any {
		it == "testAll" || it.endsWith(":testAll")
	}

	tasks.withType<Test>().configureEach {
		useJUnitPlatform()
		jvmArgs("-javaagent:${mockitoAgent.asPath}")
		if (forceTestExecution) {
			outputs.upToDateWhen { false }
		}
		testLogging {
			events("passed", "skipped", "failed")
			exceptionFormat = TestExceptionFormat.FULL
		}
	}
}

tasks.register("testAll") {
	group = "verification"
	description = "Runs tests in all modules"
	dependsOn(subprojects.map { "${it.path}:test" })
}
