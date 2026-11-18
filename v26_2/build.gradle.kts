plugins {
    id("io.papermc.paperweight.userdev")
}

java {
    disableAutoTargetJvm()
}

dependencies {
    paperweight.paperDevBundle("26.2.build.134-stable")
    implementation(project(":core"))
    implementation(project(":nms"))
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

val externalClasspath = configurations.compileClasspath.get().incoming.artifactView {
    componentFilter { it !is ProjectComponentIdentifier }
}.files

val sharedModuleJars = configurations.compileClasspath.get().incoming.artifactView {
    componentFilter { it is ProjectComponentIdentifier && it.projectPath != ":nms" }
}.files

val compileNmsOn26_2 = tasks.register<JavaCompile>("compileNmsOn26_2") {
    source(project(":nms").layout.projectDirectory.dir("src/main/java"))
    classpath = externalClasspath + sharedModuleJars
    destinationDirectory = layout.buildDirectory.dir("on26_2/nms")
}

fun classFiles(root: File): Map<String, ByteArray> = root.walkTopDown()
    .filter { it.isFile && it.name.endsWith(".class") }
    .associate { it.relativeTo(root).invariantSeparatorsPath to it.readBytes() }

val checkNmsOn26_2 = tasks.register("checkNmsOn26_2") {
    group = "verification"
    description = "Fails unless nms compiled against 26.2 gives the class files compiled against 1.21.11."
    val expected = project(":nms").layout.buildDirectory.dir("classes/java/main")
    val actual = compileNmsOn26_2.flatMap { it.destinationDirectory }
    dependsOn(":nms:compileJava", compileNmsOn26_2)
    inputs.dir(expected)
    inputs.dir(actual)
    doLast {
        val built = classFiles(expected.get().asFile)
        val rebuilt = classFiles(actual.get().asFile)
        val differing = (built.keys + rebuilt.keys).filter { !(built[it] contentEquals rebuilt[it]) }.sorted()
        if (differing.isNotEmpty()) {
            throw GradleException("nms does not link the same on 26.2:\n" + differing.joinToString("\n"))
        }
        logger.lifecycle("nms: ${built.size} class files, the same against 1.21.11 and 26.2")
    }
}

tasks.check {
    dependsOn(checkNmsOn26_2)
}
