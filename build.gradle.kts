plugins {
    application
    id("org.beryx.runtime") version "1.12.2"
}

group = "com.piorrro33"
version = "0.2dev"

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
    }
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

tasks.create("jpackageImageWin64") {
    doFirst {
        runtime {
            targetPlatform("win64") {
                setJdkHome(jdkDownload("https://github.com/AdoptOpenJDK/openjdk16-binaries/releases/download/jdk-16%2B36/OpenJDK16-jdk_x64_windows_hotspot_16_36.zip"))
            }
            jpackage {
                targetPlatformName = "win64"
                outputDir = "jpackage/${project.name}-${project.version}-${targetPlatformName}"
                imageOptions = listOf("--win-console")
            }
        }
    }
    finalizedBy("jpackageImage")
}

tasks.create("jpackageImageLinux64") {
    group = "build"
    doFirst {
        runtime {
            targetPlatform("linux-x64") {
                setJdkHome(jdkDownload("https://github.com/AdoptOpenJDK/openjdk16-binaries/releases/download/jdk-16%2B36/OpenJDK16-jdk_x64_linux_hotspot_16_36.tar.gz"))
            }
            jpackage {
                targetPlatformName = "linux-x64"
                outputDir = "jpackage/${project.name}-${project.version}-${targetPlatformName}"
            }
        }
    }
    finalizedBy("jpackageImage")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    options.release.set(16)
    // Hack for Java 16 support
    options.isIncremental = false
//    options.forkOptions.jvmArgs?.addAll(listOf("--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED"))
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}