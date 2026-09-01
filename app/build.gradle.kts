import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The upload keystore's path and credentials live in keystore.properties in the
// shared Google Play Signing Key folder (outside any repo), so no credential
// ever enters the repository. The folder is on a different drive per machine
// (D: on the LENOVO box, E: here), so both are probed. When the file is
// absent — CI, a fresh clone — the release build degrades to unsigned rather
// than failing.
val keystoreFile = listOf(
    "D:/GDrive/BSCPLC/DM (Development)/Personal Docs/Pers/Google Play Signing Key/keystore.properties",
    "E:/GDrive/BSCPLC/DM (Development)/Personal Docs/Pers/Google Play Signing Key/keystore.properties",
).map { file(it) }.firstOrNull { it.exists() }

val releaseKeystore = Properties()
if (keystoreFile != null) {
    releaseKeystore.load(keystoreFile.inputStream())
}

// The storeFile inside keystore.properties carries a drive letter that may not
// match this machine (the properties file and the keystore always sit together
// in the same folder). If the literal path does not exist, resolve the store
// file against the keystore.properties' own directory instead.
val keystoreStoreFile: java.io.File? = run {
    val kf = keystoreFile ?: return@run null
    val raw = releaseKeystore.getProperty("storeFile") ?: return@run null
    val literal = file(raw)
    if (literal.exists()) literal
    else {
        val name = raw.substringAfterLast('/').substringAfterLast('\\')
        file("${kf.parentFile.absolutePath}/$name")
    }
}
val canSignRelease = keystoreStoreFile != null &&
    releaseKeystore.containsKey("storePassword") &&
    releaseKeystore.containsKey("keyAlias") &&
    releaseKeystore.containsKey("keyPassword")

android {
    namespace = "io.github.muntasimulhaque.ninetynine"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.muntasimulhaque.ninetynine"
        minSdk = 24
        targetSdk = 37
        versionCode = 27
        versionName = "1.17"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = keystoreStoreFile
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
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
            signingConfig = if (canSignRelease) signingConfigs.getByName("release") else null
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // The Settings screen shows the version from BuildConfig.VERSION_NAME,
        // so it can never disagree with the versionName above.
        buildConfig = true
    }

    // Play encodes a dependency manifest into every artifact by default. The
    // app ships nothing it needs to advertise there — and every byte of
    // metadata an artifact carries is metadata it leaks — so leave the block
    // out of the APK and the AAB entirely.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// AGP 9's built-in Kotlin replaces the kotlin-android plugin;
// compiler options move here from the old android { kotlinOptions { } }.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}