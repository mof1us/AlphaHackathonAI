plugins { id("org.springframework.boot") }

springBoot { mainClass.set("ru.alfahack.elephants.backend.authserver.AuthorizationServerApplicationKt") }
tasks.bootJar { archiveFileName.set("authorization-server.jar") }

dependencies {
    testImplementation("org.jooq:jooq")
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":authorization"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.bootRun { workingDir = rootProject.projectDir }
