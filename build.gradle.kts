plugins {
    kotlin("jvm") version "2.1.21"
    application
}

group = "jp.yosakoi"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.apis:google-api-services-sheets:v4-rev20240319-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.30.1")
    implementation("com.google.http-client:google-http-client-gson:1.45.2")
    implementation("org.apache.commons:commons-csv:1.14.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}

application {
    mainClass.set("jp.yosakoi.sync.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
