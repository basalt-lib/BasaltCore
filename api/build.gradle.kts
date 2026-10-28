import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    `maven-publish`
}

base {
    archivesName = "basalt-api"
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

val releaseRepository: File = providers.gradleProperty("releaseRepository").map { file(it) }
    .getOrElse(layout.buildDirectory.dir("release-repository").get().asFile)

publishing {
    publications {
        create<MavenPublication>("basaltApi") {
            artifactId = "basalt-api"
            from(components["java"])
            pom {
                name = "Basalt API"
                description = "Per-player virtual worlds for Paper, made of packets"
                url = "https://github.com/basalt-lib/BasaltCore"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "basalt-lib"
                        name = "basalt-lib"
                        url = "https://github.com/basalt-lib"
                    }
                }
                scm {
                    url = "https://github.com/basalt-lib/BasaltCore"
                    connection = "scm:git:https://github.com/basalt-lib/BasaltCore.git"
                }
            }
        }
    }
    repositories {
        maven {
            name = "release"
            url = uri(releaseRepository)
        }
    }
}

val releaseChecksums = mapOf("md5" to "MD5", "sha1" to "SHA-1", "sha256" to "SHA-256", "sha512" to "SHA-512")

fun Element.children(tag: String): List<Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }

fun Element.textAt(path: List<String>): String? {
    var element: Element? = this
    for (tag in path) {
        element = element?.children(tag)?.firstOrNull()
    }
    return element?.textContent?.trim()
}

fun readXml(file: File): Element = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement

fun checksumFaults(file: File): List<String> = releaseChecksums.mapNotNull { (extension, algorithm) ->
    val sum = File(file.path + "." + extension)
    val expected = MessageDigest.getInstance(algorithm).digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    when {
        !sum.isFile -> "missing ${sum.name}"
        sum.readText().trim() != expected -> "wrong ${sum.name}"
        else -> null
    }
}

tasks.register("checkReleaseRepository") {
    group = "publishing"
    description = "Checks basalt-api of the build version in the release repository."
    val root = releaseRepository
    val version = project.version.toString()
    doLast {
        val artifact = root.resolve("io/github/basalt-lib/basalt-api")
        val folder = artifact.resolve(version)
        val metadata = artifact.resolve("maven-metadata.xml")
        val faults = mutableListOf<String>()
        if (version.endsWith("-SNAPSHOT")) {
            faults += "the version $version is a snapshot"
        }
        val files = listOf("$version.jar", "$version-sources.jar", "$version-javadoc.jar", "$version.pom", "$version.module")
            .map { folder.resolve("basalt-api-$it") } + metadata
        for (file in files) {
            if (file.isFile) {
                faults += checksumFaults(file)
            } else {
                faults += "missing ${file.relativeTo(root)}"
            }
        }
        val pom = folder.resolve("basalt-api-$version.pom")
        if (pom.isFile) {
            val model = readXml(pom)
            val expected = mapOf(
                listOf("groupId") to "io.github.basalt-lib",
                listOf("artifactId") to "basalt-api",
                listOf("version") to version,
                listOf("name") to "Basalt API",
                listOf("url") to "https://github.com/basalt-lib/BasaltCore",
                listOf("licenses", "license", "name") to "Apache-2.0",
                listOf("developers", "developer", "id") to "basalt-lib",
                listOf("scm", "url") to "https://github.com/basalt-lib/BasaltCore",
            )
            expected.forEach { (path, value) ->
                val found = model.textAt(path)
                if (found != value) {
                    faults += "pom ${path.joinToString("/")} is $found, expected $value"
                }
            }
            if (model.textAt(listOf("description")).isNullOrBlank()) {
                faults += "pom description is empty"
            }
        }
        if (metadata.isFile) {
            val listed = readXml(metadata).children("versioning").flatMap { it.children("versions") }
                .flatMap { it.children("version") }.map { it.textContent.trim() }
            if (version !in listed) {
                faults += "maven-metadata.xml does not list $version"
            }
            listed.filter { it.endsWith("-SNAPSHOT") }.forEach { faults += "maven-metadata.xml lists the snapshot $it" }
            listed.filterNot { artifact.resolve(it).isDirectory }.forEach { faults += "maven-metadata.xml lists $it without its folder" }
        }
        artifact.listFiles().orEmpty().filter { it.isDirectory && it.name.endsWith("-SNAPSHOT") }
            .forEach { faults += "snapshot folder ${it.name}" }
        if (faults.isNotEmpty()) {
            throw GradleException("release repository $root is not valid:\n" + faults.joinToString("\n"))
        }
        logger.lifecycle("release repository $root holds basalt-api $version with its sources, javadoc, pom, module and checksums")
    }
}
