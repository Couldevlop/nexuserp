import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("jacoco")
}

allprojects {
    group = "com.nexuserp"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        // Testing
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "1.0".toBigDecimal()
                }
            }
        }
    }

    // Exclude Spring config & generated classes from coverage
    tasks.withType<JacocoReport> {
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/config/**",
                        "**/infrastructure/migration/**",
                        "**/*Application.class",
                        "**/*Generated*.class"
                    )
                }
            })
        )
    }

    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.springframework.boot") {
                useVersion("3.3.4")
            }
        }
    }
}
