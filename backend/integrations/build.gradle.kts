dependencies {
    implementation(project(":domain"))
    api(project(":utils"))
    api("io.temporal:temporal-sdk:1.38.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("io.temporal:temporal-testing:1.38.0")
}
