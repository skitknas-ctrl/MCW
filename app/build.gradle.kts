import java.time.LocalDate

plugins {
    id("com.android.application")
}

// Byggdatum som visas i appen
val buildDate: String = LocalDate.now().toString()

android {
    namespace = "se.dick.mcw"
    compileSdk = 36
    defaultConfig {
        applicationId = "se.dick.mcw"
        minSdk = 31
        targetSdk = 36
        // CI skickar -PbuildNumber=<körningsnummer>; versionen blir då 1.0.<n>
        val baseVersion = "1.0"
        val buildNumber = (project.findProperty("buildNumber") as String?)?.toIntOrNull()
        // Officiell utgåva. F-Droid bygger från taggen release-<versionName>, utan buildNumber.
        versionCode = 1100
        versionName = "1.1.0"
        if (buildNumber != null) {
            // testbygge från CI
            versionCode = 100 + buildNumber
            versionName = "$baseVersion.$buildNumber"
        }
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
    }
    signingConfigs {
        // Nyckel som ligger i repot så att varje CI-bygge kan uppdatera det förra (Obtainium).
        // Byt till en hemlig nyckel om appen ska publiceras brett.
        create("ci") {
            storeFile = file("ci.keystore")
            storePassword = "android"
            keyAlias = "mcw"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ci")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}
