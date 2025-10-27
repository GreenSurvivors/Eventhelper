plugins {
    `java-library`
    //java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.raphimc.class-token-replacer") version "1.1.7" // allows to replace strings in classes
}

group = "de.greensurvivors"
version = buildString {
    append(project.properties["pluginVersion"])

    if ((project.properties["release"] as String).toBoolean().not()) {
        append("-Snapshot")
    }

    append("+${project.properties["mcVersion"]}")
}

description = "Helper for all kinds of Events."

// we only work with paper and downstream!
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 17 installed for example.
    // If you need to compile to for example JVM 8 or 17 bytecode, adjust the 'release' option below and keep the toolchain at 21.
    toolchain.languageVersion = JavaLanguageVersion.of("${rootProject.properties["javaVersion"]}")
    sourceCompatibility = JavaVersion.toVersion(rootProject.properties["javaVersion"]!!)
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
    paperweight.paperDevBundle("${project.properties["mcVersion"]}-R0.1-SNAPSHOT")
    compileOnly(
        "com.sk89q.worldguard",
        "worldguard-bukkit",
        "${project.properties["worldguardVersion"]}"
    ) //newest worldguard version
    api("com.github.ben-manes.caffeine", "caffeine", "${project.properties["caffeineVersion"]}") // caches
    compileOnly(
        "de.greensurvivors",
        "SimpleQuests",
        "${project.properties["simpleQuestsVersion"]}"
    ) // installed locally as we depend on a dev version
    api("org.apache.commons", "commons-collections4", "${project.properties["commons-collections4Version"]}")
}

// set up the versions to get replaced in the Loader class
sourceSets {
    main {
        classTokenReplacer {
            property($$"${version}", version)
            property($$"${caffeineVersion}", "${project.properties["caffeineVersion"]}")
            property($$"${commons-collections4Version}", "${project.properties["commons-collections4Version"]}")
        }
    }
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
            modrinth("worldedit", "${project.properties["worldeditVersionRunTask"]}")
            modrinth("worldguard", "${project.properties["worldguardVersionRunTask"]}")
        }
    }
}