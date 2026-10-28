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
