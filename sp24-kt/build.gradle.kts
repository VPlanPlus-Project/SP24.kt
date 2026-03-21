plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.signing)
}

val libraryVersion = project.findProperty("version") as? String ?: "unspecified"

if (version == "unspecified") {
    println("WARN: Version not set (using unspecified)")
}

group = "plus.vplan.lib"
version = libraryVersion

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "plus.vplan.lib"
            artifactId = "sp24"
            version = libraryVersion

            from(components["kotlin"])

            pom {
                name = "SP24.kt"
                description = "A KMP-compatible wrapper for Stundenplan24.de"
                url = "https://github.com/VPlanPlus-Project/SP24.kt"

                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/VPlanPlus-Project/SP24.kt")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }

        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}