# SP24.kt

[![Gradle Test & Publish](https://github.com/VPlanPlus-Project/SP24.kt/actions/workflows/publish.yaml/badge.svg)](https://github.com/VPlanPlus-Project/SP24.kt/actions/workflows/publish.yaml)
![Maven Central Version](https://img.shields.io/maven-central/v/plus.vplan.lib/sp24-kt?strategy=highestVersion&style=flat-square&label=Current%20version)




SP24.kt is a Kotlin library for building software that needs data
from [stundenplan24.de](https://stundenplan24.de), developed by Indiware.

# About this library

While working on [VPlanPlus](https://github.com/VPlanPlus-Project/app), we reverse-engineered the
network requests used by stundenplan24.de and
quickly realized the API isn’t exactly pleasant to deal with. During testing — and honestly still
sometimes — we kept running into weird, school-specific quirks in the XML data it serves.

Since multiple apps need to work with this data reliably, we decided to move all the parsing and
cleanup logic into a separate library. That way, we only have to deal with the messy stuff in one
place, and updates can be rolled out everywhere just by bumping the version. As a nice bonus, others
can use it too without going through the same headaches.

# Getting started
## Finding the latest version
Checkout the [latest release](https://github.com/VPlanPlus-Project/SP24.kt/releases/latest). Use 
this version number in your `libs.versions.toml` or `build.gradle.kts`:

```kotlin
implementation("plus.vplan.lib:sp24-kt:VERSION")
```

# Using the library
Documentation following soon.
We'll [dogfood](https://en.wikipedia.org/wiki/Eating_your_own_dog_food) this library in our own
projects and improve and extend it as we go.

## Some code examples

To get started, create a client.

```kotlin
val authentication = Authentication(
    sp24SchoolId = "10000000",
    username = "schueler",
    password = "passwort"
)

val client = Stundenplan24Client(authentication = authentication)
```

### Testing the connection

Having the client, you can load whatever you want. However, it's always a good idea to verify,
whether the credentials match.

```kotlin
val result = client.testConnection() // Success, Error, InvalidCredentials or NotExists
```
See [TestConnection.kt](https://github.com/VPlanPlus-Project/SP24.kt/blob/e307d2fce1cec79d4beaa0a60b7f0339390d8b3f/library/src/jvmTest/kotlin/TestConnection.kt) from the tests for example.

### Getting the name of a school
Just to give you an idea, retrieving a schools name can involve multiple network requests, because
not every school supports every feature. Some schools use modules that others don't, making
standardized data complicated. But SP24.kt provides a convenient function.

```kotlin
val schoolName = (client.getSchoolName() as? Response.Success)?.data
```
See [SchoolNameTest.kt](https://github.com/VPlanPlus-Project/SP24.kt/blob/196914cb3ad63bb2b840a2b37e4ac99a74d80538/sp24-kt/src/jvmTest/kotlin/SchoolNameTest.kt) to learn mode.
