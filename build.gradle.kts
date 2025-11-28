import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    id("java")
    id("com.mineinabyss.conventions.kotlin.jvm") version "2.2.0"
    alias(idofrontLibs.plugins.mia.autoversion)
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.2.0"
}

// Capture current Git branch
val gitBranch: String = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}.standardOutput.asText.get().trim()

// Capture short Git hash
val gitHash: String = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

val pluginVersion: String = project.property("version").toString().let { if (gitBranch == "master") it.removeSuffix("-dev") else it }
val copyJarPath = project.findProperty("nexo_plugin_path").toString()
val jarName = "${project.name}-${pluginVersion}${if (gitBranch != "master") "-${gitHash}" else ""}.jar"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.nexomc.com/snapshots")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.codemc.org/repository/maven-public/")
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("com.nexomc:nexo:1.15.0")
    compileOnly("org.popcraft:bolt-common:1.1.31")
    compileOnly("org.popcraft:bolt-bukkit:1.1.31")

    implementation("org.bstats:bstats-bukkit:3.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    shadowJar {
        relocate("kotlinx.", "com.nexomc.libs.kotlinx.")
        relocate("kotlin.", "com.nexomc.libs.kotlin.")
        fun shade(string: String) = relocate(string, "com.nexomc.libs")
        shade("org.bstats")

        manifest {
            attributes(
                mapOf(
                    "Built-By" to System.getProperty("user.name"),
                    "Version" to pluginVersion,
                    "Created-By" to "Gradle ${gradle.gradleVersion}",
                    "Build-Jdk" to "${System.getProperty("java.version")} ${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")}",
                    "Compiled" to (project.findProperty("nexo_compiled")?.toString() ?: "true").toBoolean(),
                    "CI" to (System.getenv("CI") ?: "false").toBoolean(),
                )
            )
        }

        destinationDirectory.set(File(copyJarPath))
        archiveFileName.set(jarName)
    }

    build {
        dependsOn(shadowJar)
        doLast {
            val pluginJar = File(copyJarPath).listFiles { file -> file.extension == "jar" }?.maxByOrNull { it.lastModified() } ?: return@doLast
            project.properties.filter { it.key.startsWith("NexoBoltAddon") && it.key.endsWith("plugin_path") }.values.forEach {
                val jarFiles = File(it.toString()).listFiles { file ->
                    file.extension == "jar" && file.name.startsWith(project.name.substringBefore("-"))
                            && file.name != jarName
                }?.sortedByDescending { it.lastModified() }
                if (it.toString() != copyJarPath) pluginJar.copyTo(File(it.toString(), pluginJar.name), true)
                jarFiles?.forEach(File::delete)
            }
        }
    }
}


paperPluginYaml {
    main = "com.nexomc.nexo_bolt_addon.NexoBoltAddon"
    name = "NexoBoltAddon"
    apiVersion = "1.21"
    this.version = pluginVersion
    authors.add("boy0000")
    foliaSupported = true

    dependencies.server("Nexo", PaperPluginYaml.Load.BEFORE, required = true)
    dependencies.server("Bolt", PaperPluginYaml.Load.BEFORE, required = true)
}
