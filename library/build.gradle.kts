plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "plus.vplan.lib"
version = project.findProperty("version") ?: "unspecified"

kotlin {
    jvm()
    android {
        namespace = "plus.vplan.lib.sp24"
        compileSdk = 36
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)

            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlin.test)
        }
    }
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}