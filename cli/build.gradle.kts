plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.bookabase"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("commons-validator:commons-validator:1.7")
    implementation("de.m3y.kformat:kformat:0.10")
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}