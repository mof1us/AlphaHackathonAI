plugins { id("org.springframework.boot") }

springBoot { mainClass.set("ru.alfahack.elephants.backend.temporalrunner.TemporalRunnerApplicationKt") }
tasks.bootJar { archiveFileName.set("temporal-runner.jar") }

dependencies {
    testImplementation("org.jooq:jooq")
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":integrations"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}

tasks.bootRun { workingDir = rootProject.projectDir }
