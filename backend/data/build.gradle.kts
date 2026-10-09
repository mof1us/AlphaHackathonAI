import org.flywaydb.core.Flyway

plugins { id("nu.studer.jooq") version "10.2.1" }

buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.flywaydb:flyway-core:10.15.2")
        classpath("org.flywaydb:flyway-database-postgresql:10.15.2")
        classpath("org.postgresql:postgresql:42.7.3")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":utils"))
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jooq:jooq")
    implementation("org.jooq:jooq-kotlin")
    implementation("org.jooq:jooq-reactor-extensions")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    jooqGenerator("org.postgresql:postgresql:42.7.3")
}

val dotEnv = rootProject.file(".env").takeIf { it.isFile }?.readLines()
    ?.map(String::trim)
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
    ?.associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
    .orEmpty()

fun databaseSetting(name: String, default: String): String =
    providers.environmentVariable(name).orElse(dotEnv[name] ?: default).get()

val databaseHost = databaseSetting("DB_HOST", "localhost")
val databasePort = databaseSetting("DB_PORT", "5432")
val databaseName = databaseSetting("DB_NAME", "elephants")
val databaseUser = databaseSetting("DB_USER", "postgres")
val databasePassword = databaseSetting("DB_PASSWORD", "postgres")
val databaseUrl = "jdbc:postgresql://$databaseHost:$databasePort/$databaseName"

jooq {
    version.set("3.21.5")
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = databaseUrl
                    user = databaseUser
                    password = databasePassword
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        excludes = "flyway_schema_history"
                    }
                    target.apply {
                        packageName = "ru.alfahack.elephants.backend.data.jooq"
                        directory = layout.projectDirectory.dir(
                            "src/main/kotlin/ru/alfahack/elephants/backend/data/jooq",
                        ).asFile.absolutePath
                    }
                }
            }
        }
    }
}

tasks.named("compileKotlin") { mustRunAfter("generateJooq") }

/** Применяет миграции только при явном вызове задачи, не при сборке приложения. */
abstract class FlywayMigrationTask : DefaultTask() {
    @get:Input abstract val jdbcUrl: Property<String>
    @get:Input abstract val username: Property<String>
    @get:Input abstract val password: Property<String>
    @get:Input abstract val migrationsLocation: Property<String>

    @TaskAction
    fun migrate() {
        Flyway.configure()
            .dataSource(jdbcUrl.get(), username.get(), password.get())
            .locations(migrationsLocation.get())
            .load().migrate()
    }
}

tasks.register<FlywayMigrationTask>("migrate") {
    group = "flyway"
    description = "Applies migrations to the single application database."
    jdbcUrl.set(databaseUrl)
    username.set(databaseUser)
    password.set(databasePassword)
    migrationsLocation.set("filesystem:${layout.projectDirectory.dir("src/main/resources/migrations").asFile}")
}
