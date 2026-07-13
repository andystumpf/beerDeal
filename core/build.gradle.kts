plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.beerdeal"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnit()
}
