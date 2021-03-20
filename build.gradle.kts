plugins {
    application
    id("org.beryx.runtime") version "1.12.2"
}

group = "com.piorrro33"
version = "0.2-SNAPSHOT"

application {
    mainClass.set("com.github.piorrro33.hd6tools.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

runtime {
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    modules.addAll("java.base")
    jpackage {
        appVersion = "0.2"
        imageOptions = listOf("--win-console")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    options.release.set(16)
    // Hack for Java 16 support
    options.forkOptions.jvmArgs?.addAll(listOf("--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.6.1")
    annotationProcessor("info.picocli:picocli-codegen:4.6.1")

    testImplementation(platform("org.junit:junit-bom:5.7.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}