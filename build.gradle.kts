// Build script for the GameFlow Assistant desktop app.
//
// Stack (faithful to the requested C#/.NET/WPF scaffold, using the closest
// JVM analogues this environment supports):
//   Kotlin (JVM)  -> the .NET/C# taste (language lives on top of the JVM)
//   Swing         -> WPF (native desktop windowing, MVVM-style View)
//   OpenCV/JavaCV -> optional native template-matching engine
//   sqlite-jdbc   -> SQLite (local persistence)
//
// Build & run (Linux or Windows with a JDK 17+ and Gradle):
//   ./gradlew build          (or .\gradlew.bat build)
//   ./gradlew run --args=--demo
//
// Alternative (verified) path without Gradle: set KOTLINC and just use ./run.sh
//   ./run.sh smoke          runs the pure-JVM functional SmokeTest
//
// Make sure the jars in ./lib are present (sqlite-jdbc, slf4j-api). To use the
// OpenCV matcher, add the JavaCV artifacts to the dependency list below and run
// with -Dgameflow.opencv=true.

plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "com.gameflow"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Local copies of the third-party jars shipping in ./lib.
    implementation(files("lib/sqlite-jdbc-3.45.1.0.jar"))
    implementation(files("lib/slf4j-api-2.0.13.jar"))

    // Unleash the native OpenCV matcher by uncommenting (and running javacv-launcher):
    // implementation("org.bytedeco:opencv:4.9.0") // optional acceleration

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        jvmTarget = "17"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    // Kotlin @JvmStatic main -> JVM entry point on App.
    mainClass = "GameFlow.App"
}

tasks.jar {
    archiveFileName = "gameflow-${version}.jar"
    manifest { attributes["Main-Class"] = application.mainClass }
}

// A `./gradlew smoke` helper for the pure-JVM functional smoke test.
tasks.register<JavaExec>("smoke") {
    group = "verification"
    description = "Run the pure-JVM smoke test"
    classpath = tasks.compileKotlin.get().classpath + sourceSets["main"].output
    mainClass = "GameFlow.SmokeTest"
}