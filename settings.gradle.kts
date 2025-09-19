pluginManagement {
    // when using additional gradle plugins like shadow,
    // add their repositories to this list!
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://files.minecraftforge.net/maven/") }
        gradlePluginPortal()
    }
}

rootProject.name = "ritchiesfirearmengine"

