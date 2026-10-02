plugins {
    id("com.android.application")
}

android {
    namespace = "com.fakkerni.reminder"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.fakkerni.reminder"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.5.0"
        testInstrumentationRunner = "com.fakkerni.reminder.SmokeInstrumentation"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
