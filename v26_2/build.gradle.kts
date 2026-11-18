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
