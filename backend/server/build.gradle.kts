plugins { id("org.springframework.boot") }

springBoot {
    mainClass.set("ru.alfahack.elephants.backend.server.BackendApplicationKt")
    buildInfo()
}
tasks.bootJar { archiveFileName.set("server.jar") }

dependencies {
    testImplementation("org.jooq:jooq")
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":authorization"))
    implementation(project(":integrations"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
}

tasks.bootRun { workingDir = rootProject.projectDir }
