plugins {
    id("net.fabricmc.fabric-loom") version "1.17.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

base {
    archivesName = project.findProperty("archives_base_name") as String
    version = project.findProperty("mod_version") as String
    group = project.findProperty("maven_group") as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        name = "Bawnorton"
        url = uri("https://maven.bawnorton.com/releases")
    }
    maven {
        name = "babbaj"
        url = uri("https://babbaj.github.io/maven/")
    }
    flatDir {
        dirs("libs")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.findProperty("minecraft_version") as String}")
    implementation("net.fabricmc:fabric-loader:${project.findProperty("loader_version") as String}")
    implementation("meteordevelopment:meteor-client:${project.findProperty("meteor_version") as String}-SNAPSHOT")
    implementation("meteordevelopment:baritone:${project.findProperty("baritone_version") as String}-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.10.1")
    include("com.nikoverflow:exploitpreventer:1.1.0")
    runtimeOnly("dev.babbaj:nether-pathfinder:1.4.1")
    runtimeOnly("net.fabricmc.fabric-api:fabric-api:${project.findProperty("fabric_api_version") as String}")
    runtimeOnly("com.nikoverflow:exploitpreventer:1.1.0")
    runtimeOnly("com.nikoverflow:ExploitPreventer-API:1.0.0")
    runtimeOnly("dev.lukebemish:opensesame-core:0.8.1")
    include(implementation(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:0.3.7-beta.1")!!)!!)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
        )
        inputs.properties(propertyMap)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }
    jar {
        val licenseSuffix = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${licenseSuffix}" }
        }
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
    }
}
