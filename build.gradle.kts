plugins {
    id("java")
}

group = "tide"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fifesoft:rsyntaxtextarea:3.5.2")
    implementation("com.formdev:flatlaf:3.7.1")
}

tasks.test {
    useJUnitPlatform()
}