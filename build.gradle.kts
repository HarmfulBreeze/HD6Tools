plugins {
    application
    id("org.graalvm.buildtools.native") version "0.9.9"
}

group = "com.piorrro33"
version = "0.2dev"

application {
    mainClass.set("com.github.piorrro33.hd6tools.Main")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<JavaExec> {
    standardInput = System.`in` // Allows user input while running from Gradle
    outputs.upToDateWhen { false }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.6.2")
    annotationProcessor("info.picocli:picocli-codegen:4.6.2")

    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("-H:+AddAllCharsets")
            imageName.set(project.name)
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
            verbose.set(true)
        }
    }
}
