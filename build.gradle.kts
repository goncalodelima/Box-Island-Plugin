import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

tasks {

    named<ShadowJar>("shadowJar"){
        relocate("co.aikar.commands", "pt.gongas.box.lib.aikar.commands")
        relocate("co.aikar.locales", "pt.gongas.box.lib.aikar.locales")
        relocate("dev.triumphteam.gui", "pt.gongas.box.lib.triumph.gui")
        relocate("dev.triumphteam.core", "pt.gongas.box.lib.triumph.core")
        exclude("META-INF/versions/**")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    build {
        dependsOn(shadowJar)
    }
}


repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.triumphteam.dev/snapshots") // GUI API
    maven("https://repo.aikar.co/content/groups/aikar/") // Aikar
    maven("https://repo.purpurmc.org/snapshots") // Purpur
    maven("https://repo.papermc.io/repository/maven-public/") // paperweight, Velocity
    maven("https://repo.codemc.org/repository/nms/") // CraftBukkit + NMS
    maven("https://repo.codemc.org/repository/maven-public/") // Item-NBT-API
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.codemc.io/repository/maven-releases/") // PacketEventsAPI
    maven("https://repo.codemc.io/repository/maven-snapshots/") // PacketEventsAPI
    maven("https://mvn.lumine.io/repository/maven-public/") // MythicMobs
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/") // AdvancedSlimePaper
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

group = "pt.gongas"
version = "1.0.0"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly("net.kyori:adventure-text-minimessage:4.24.0")
    implementation("dev.triumphteam:triumph-gui-paper:3.1.13-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    //compileOnly("com.github.decentsoftware-eu:decentholograms:2.9.6")
    //compileOnly("de.tr7zw:item-nbt-api-plugin:2.15.1")
    //compileOnly("io.lumine:Mythic-Dist:5.9.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")
    implementation("com.infernalsuite.asp:file-loader:4.0.0-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

bukkit {
    name = "box-plugin"
    version = "${project.version}"
    main = "pt.gongas.box.BoxPlugin"
    depend = listOf("MinecraftSolutions", "redis-plugin")
    author = "ReeachyZ_"
    website = "https://github.com/goncalodelima"
    description = "Box Plugin"
    apiVersion = "1.21"
}