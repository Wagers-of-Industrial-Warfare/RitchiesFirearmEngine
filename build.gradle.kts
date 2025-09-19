plugins {
    id("dev.architectury.loom") version "1.11.+"
    id("maven-publish")
}

base {
    archivesName.set(prop("mod_id"))
}

version = "${prop("mod_version")}+mc.${prop("minecraft_version")}${if (env.nightly()) "-build.${env.buildNumber()}" else ""}${if (env.ci()) "" else "-dev"}"
group = prop("mod_group")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

loom {
    silentMojangMappingsLicense()

    forge {
        mixinConfigs = listOf(
            "${prop("mod_id")}.mixins.json"
        )
    }
}

repositories {
    strictMaven("https://maven.parchmentmc.org", "org.parchmentmc.data")
    strictMaven("https://cursemaven.com", "curse.maven")
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // to change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    mappings(
        loom.layered {
            officialMojangMappings { nameSyntheticMembers = false }
            parchment("org.parchmentmc.data:parchment-${prop("minecraft_version")}:${prop("parchment_version")}@zip")
        }
    )

    forge("net.minecraftforge:forge:${prop("minecraft_version")}-${prop("forge_version")}")

    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0") {})
    implementation(include("io.github.llamalad7:mixinextras-forge:0.5.0") {})

    // modRuntimeOnly("mezz.jei:jei-1.20.1:${prop("jei_version")}")
    modRuntimeOnly("curse.maven:moonlight-499980:${prop("moonlight_cf_file")}")
    modRuntimeOnly("curse.maven:dummymod-225738:${prop("dummy_mod_cf_file")}")
    modRuntimeOnly("curse.maven:configured-457570:${prop("configured_cf_file")}")
}

tasks.processResources {
    notCompatibleWithConfigurationCache("accesses Project")
    filesMatching("META-INF/mods.toml") {
        expand(mapOf(
                "minecraft_version" to prop("minecraft_version"),
                "minecraft_version_range" to prop("minecraft_version_range"),
                "forge_version" to prop("forge_version"),
                "forge_version_range" to prop("forge_version_range"),
                "loader_version_range" to prop("loader_version_range"),
                "mod_id" to prop("mod_id"),
                "mod_name" to prop("mod_name"),
                "mod_license" to prop("mod_license"),
                "mod_version" to prop("mod_version"),
                "mod_authors" to prop("mod_authors"),
                "mod_description" to prop("mod_description"),
                "version" to project.version
            )
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

java {
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to prop("mod_id"),
                "Specification-Vendor" to prop("mod_authors"),
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to prop("mod_authors"),
                //"Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(java.util.Date())
            )
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.remapJar)
            artifact(tasks.remapSourcesJar)
            group = prop("mod_group")
            artifactId = prop("mod_id")
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/wagers-of-industrial-warfare/ritchiesfirearmengine")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "realRobotixMaven"
            url = uri("https://maven.realrobotix.me/ritchiesfirearmengine")
            credentials(PasswordCredentials::class)
        }
        mavenLocal()
    }
}

