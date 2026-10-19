dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":nms"))
}

val bundledModules = configurations.runtimeClasspath.get().incoming.artifactView {
    componentFilter { it is ProjectComponentIdentifier }
}.files

tasks.jar {
    archiveBaseName = "BasaltCore"
    manifest {
        attributes("paperweight-mappings-namespace" to "mojang")
    }
    from(bundledModules.elements.map { jars -> jars.map { zipTree(it.asFile) } }) {
        exclude("META-INF/MANIFEST.MF")
    }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("paper-plugin.yml") {
        expand("version" to version)
    }
}

tasks.test {
    systemProperty("basalt.version", project.version.toString())
}
