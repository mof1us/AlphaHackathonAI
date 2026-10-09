import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    base
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "ru.alfahack.elephants"
    version = "devel"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")

    extra["jooq.version"] = "3.21.5"
    configure<DependencyManagementExtension> {
        imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1") }
    }
    configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }
    configure<KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }
    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
        add("testImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-test")
        add("testImplementation", "io.mockk:mockk:1.14.11")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

tasks.named("build") { dependsOn(subprojects.map { "${it.path}:build" }) }
tasks.named("check") { dependsOn(subprojects.map { "${it.path}:check" }) }
tasks.named("clean") { dependsOn(subprojects.map { "${it.path}:clean" }) }
