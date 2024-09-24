//import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    //java
    id("io.papermc.paperweight.userdev") version "1.7.2"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
}

group = "de.greensurvivors"
version = "2.0.0-SNAPSHOT"
description = "Helper for all kinds of Events."
// this is the minecraft major version. If you need a subversion like 1.20.1,
// change it in the dependencies section as this is also used as the api version of the plugin.yml
val mainMCVersion by extra("1.20")

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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
    paperweight.paperDevBundle("$mainMCVersion.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.9" /*"7.1.0-SNAPSHOT" // newest worldguard version*/)
    api("com.github.ben-manes.caffeine:caffeine:3.1.8") // caches
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        expand(project.properties)
    }


    runServer {
        downloadPlugins {
            // make sure to double check the version id on the Modrinth version page
            // for 1.21+ :
            //modrinth("worldguard", "J66QOTLZ")
            // for 1.20.4 --> 1.21
            url("https://mediafilez.forgecdn.net/files/4675/318/worldguard-bukkit-7.0.9-dist.jar")
            // we have to ignore all newer versions since they depend on java 21
            modrinth("worldedit", "JzCMkGax")
        }
    }
}