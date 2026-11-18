dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":nms"))
    implementation(project(":v1_21_11"))
    implementation(project(":v26_2"))
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
    systemProperty("basalt.root", rootDir.absolutePath)
    inputs.files(fileTree(rootDir) {
        include("**/*.java", "**/*.kts", "**/*.yml", "**/*.yaml", "**/*.properties")
        exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", ".git/**", ".idea/**", "run/**", "out/**")
    }).withPropertyName("seamSources").withPathSensitivity(PathSensitivity.RELATIVE)
}

val paperOn26_2 = configurations.create("paperOn26_2") {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

dependencies {
    paperOn26_2("io.papermc.paper:paper-api:26.2.build.134-stable")
    paperOn26_2("org.jetbrains:annotations:26.1.0")
}

val moduleJars = configurations.compileClasspath.get().incoming.artifactView {
    componentFilter { it is ProjectComponentIdentifier }
}.files

val compileApiOn26_2 = tasks.register<JavaCompile>("compileApiOn26_2") {
    source(project(":api").layout.projectDirectory.dir("src/main/java"))
    classpath = paperOn26_2
    destinationDirectory = layout.buildDirectory.dir("on26_2/api")
}

val compilePluginOn26_2 = tasks.register<JavaCompile>("compilePluginOn26_2") {
    source(sourceSets.main.get().java)
    classpath = paperOn26_2 + moduleJars
    destinationDirectory = layout.buildDirectory.dir("on26_2/plugin")
}

fun classFiles(root: File): Map<String, ByteArray> = root.walkTopDown()
    .filter { it.isFile && it.name.endsWith(".class") }
    .associate { it.relativeTo(root).invariantSeparatorsPath to it.readBytes() }

val checkBytecodeOn26_2 = tasks.register("checkBytecodeOn26_2") {
    group = "verification"
    description = "Fails unless api and plugin compiled against paper-api 26.2 give the class files compiled against 1.21.11."
    val pairs = mapOf(
        "api" to (project(":api").layout.buildDirectory.dir("classes/java/main") to compileApiOn26_2.flatMap { it.destinationDirectory }),
        "plugin" to (layout.buildDirectory.dir("classes/java/main") to compilePluginOn26_2.flatMap { it.destinationDirectory }))
    dependsOn(":api:compileJava", tasks.compileJava, compileApiOn26_2, compilePluginOn26_2)
    pairs.values.forEach { (expected, actual) ->
        inputs.dir(expected)
        inputs.dir(actual)
    }
    doLast {
        pairs.forEach { (module, dirs) ->
            val built = classFiles(dirs.first.get().asFile)
            val rebuilt = classFiles(dirs.second.get().asFile)
            val differing = (built.keys + rebuilt.keys).filter { !(built[it] contentEquals rebuilt[it]) }.sorted()
            if (differing.isNotEmpty()) {
                throw GradleException("$module does not link the same on 26.2:\n" + differing.joinToString("\n"))
            }
            logger.lifecycle("$module: ${built.size} class files, the same against 1.21.11 and 26.2")
        }
    }
}

tasks.check {
    dependsOn(checkBytecodeOn26_2)
}
