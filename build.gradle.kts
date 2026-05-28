plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "fr.bobiche"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://maven.enginehub.org/repo/") {
        name = "enginehub-repo"
}
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.20")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
  runServer {
    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion("1.20")
  }
}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

val shadePrefix = "fr.bobiche.bb_parcels.libs"

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.fasterxml.jackson", "$shadePrefix.jackson")
    relocate("com.zaxxer.hikari", "$shadePrefix.hikari")
    // Do not relocate org.mariadb: JDBC ServiceLoader (META-INF/services/java.sql.Driver) breaks and
    // Hikari reports "Failed to get driver instance for jdbcUrl=...".
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
