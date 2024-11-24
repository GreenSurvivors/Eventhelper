plugins {
    `java-library`
    //java
    id("io.papermc.paperweight.userdev") version "1.7.3"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
}

group = "de.greensurvivors"
version = "2.0.2-SNAPSHOT"
description = "Helper for all kinds of Events."
val mcVersion by extra("1.21.3")

// we only work with paper and downstream!
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenLocal()

    //paper
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    //world guard
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    paperweight.paperDevBundle("$mcVersion-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.1.0-SNAPSHOT") //newest worldguard version
    api("com.github.ben-manes.caffeine", "caffeine", "3.1.8") // caches
    compileOnly("de.greensurvivors", "SimpleQuests", "2.0.0") // installed locally as we depend on a dev version
    api("org.apache.commons", "commons-collections4", "4.5.0-M2")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        expand(project.properties)
    }


    runServer {
        downloadPlugins {
            // make sure to double-check the version id on the Modrinth version page
            modrinth("worldguard", "r9yT7tvE")
            modrinth("worldedit", "lynJ1VTU")
        }
    }
}