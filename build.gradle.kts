
plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.1"
    application
}

group = "com.github.foxolotl"
version = "0.1"

repositories {
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.1.0")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
}

tasks {
    application {
        mainClass.set("com.github.foxolotl.koro8.MainKt")
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
            jvmTarget = "1.8"
        }
    }

    val minifiedJar = register<proguard.gradle.ProGuardTask>("minifiedJar") {
        dependsOn(shadowJar)
        configuration("proguard-rules.pro")
        injars("build/libs/koro8-$version-all.jar")
        outjars("build/libs/koro8-$version-min.jar")
        libraryjars(System.getProperty("java.home") + "/jmods/java.base.jmod")
        libraryjars(System.getProperty("java.home") + "/jmods/java.desktop.jmod")
        libraryjars(System.getProperty("java.home") + "/lib/rt.jar")
    }

    build {
        finalizedBy(minifiedJar)
    }
}
