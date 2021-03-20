plugins {
    application
}

group = "com.piorrro33"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.github.piorrro33.hd6tools.Main")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
    options.release.set(15)
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