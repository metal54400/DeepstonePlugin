# DeepstonePlugin

Voici les dépendance du plugin Deepstone:

    repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = "https://repo.papermc.io/repository/maven-public/"
    }
    maven {
        name = "vault"
        url = "https://jitpack.io"
    }
    maven { url = 'https://repo.extendedclip.com/content/repositories/placeholderapi/' }
    maven { url = 'https://repo.essentialsx.net/releases/' }
    maven {
        url = "https://repo.glaremasters.me/repository/bloodshot/"
    }
    maven {
        url = uri("https://repo.minebench.de/")
       }
    }

    dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly "me.clip:placeholderapi:2.11.6"

    // EssentialsX (pour lire l'état AFK)
    compileOnly("net.essentialsx:EssentialsX:2.21.1") {
        exclude group: "org.spigotmc", module: "spigot-api"
        exclude group: "org.bukkit", module: "bukkit"
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7"){
        exclude group: "org.bukkit", module: "bukkit"
       }
    compileOnly "com.github.GriefPrevention:GriefPrevention:16.18.2"
    compileOnly("com.acrobot.chestshop:chestshop:3.12.2")
    }
    

