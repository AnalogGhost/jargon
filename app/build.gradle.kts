import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Load signing credentials from local.properties (never committed to git)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.hackerapps.jargon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hackerapps.jargon"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Only configured when keystore properties exist in local.properties.
        // F-Droid builds unsigned and applies their own signature.
        create("release") {
            val storeFile = localProps["storeFile"] as String?
            if (storeFile != null) {
                this.storeFile     = file(storeFile)
                this.storePassword = localProps["storePassword"] as String
                this.keyAlias      = localProps["keyAlias"] as String
                this.keyPassword   = localProps["keyPassword"] as String
            }
        }
    }

    flavorDimensions += "store"

    productFlavors {
        create("foss") {
            dimension = "store"
        }
        create("play") {
            dimension = "store"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (localProps["storeFile"] != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions { jvmTarget = "21" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// AGP embeds a "Dependency metadata" block in the APK signing block by default, feeding
// Google Play Console's SDK Index. F-Droid's own scanner flags it as an extra signing
// block, so disable it for the foss flavor only -- the play flavor keeps it since that's
// exactly the Play Store plumbing it's for.
androidComponents {
    beforeVariants(selector().withFlavor("store" to "foss")) { variantBuilder ->
        variantBuilder.dependenciesInfo.includeInApk = false
        variantBuilder.dependenciesInfo.includeInBundle = false
    }
}

// ART baseline profile generation (assets/dexopt/baseline.prof(m)) is not reproducible:
// AGP's ArtProfile.kt iterates a HashMap<DexFile, DexFileData> without sorting when
// serializing some profile formats, so byte order can differ build-to-build even with
// otherwise identical output. Disabling it is the documented workaround —
// see https://gist.github.com/obfusk/61046e09cee352ae6dd109911534b12e
// Only F-Droid (foss flavor) needs reproducibility; Play builds keep the profile for
// better startup performance.
tasks.configureEach {
    if (name.contains("ArtProfile") && !name.contains("Play")) {
        enabled = false
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
