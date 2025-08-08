plugins {
    id("java")
}

group = "org.figuramc.figura"
version = "rerewrite+mc1.21.5"

repositories {
    mavenCentral()
}

dependencies {

    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")
}

tasks.test {
    useJUnitPlatform()
}