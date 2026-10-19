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
}

publishing {
    publications {
        create<MavenPublication>("basaltApi") {
            artifactId = "basalt-api"
            from(components["java"])
        }
    }
}
