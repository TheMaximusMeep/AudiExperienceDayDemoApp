plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val showcaseVersionCode = providers.gradleProperty("showcase.versionCode").get().toInt()
val showcaseVersionName = providers.gradleProperty("showcase.versionName").get()

// The file name is what gets uploaded to the portal: two versions side by side must not look alike.
base { archivesName = "partner-showcase-$showcaseVersionName-vc$showcaseVersionCode" }

android {
    namespace = "technology.cariad.partnershowcase"
    compileSdk = 37

    defaultConfig {
        applicationId = "technology.cariad.partnershowcase"
        // Android 11: Car.createCar() with a lifecycle listener. Every AAOS car in the field is newer.
        minSdk = 30
        targetSdk = 34
        versionCode = showcaseVersionCode
        versionName = showcaseVersionName
    }

    // The Car API (CarPropertyManager, CarUxRestrictionsManager) is not a Maven dependency: it ships
    // with the SDK platform as an optional library and with the car's system image at runtime.
    useLibrary("android.car")

    signingConfigs {
        // A test key, committed on purpose; see keystores/README.md.
        create("showcase") {
            storeFile = rootProject.file("keystores/public-demo.jks")
            storePassword = "public-demo"
            keyAlias = "public-demo"
            keyPassword = "public-demo"
        }
    }

    buildTypes {
        // Same certificate for debug and release, so any build replaces any other on stage.
        debug { signingConfig = signingConfigs.getByName("showcase") }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("showcase")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
