import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.pfitzer.tootsie"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pfitzer.tootsie"
        minSdk = 31
        targetSdk = 37
        versionCode = 4
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing from keystore.properties (local, not in git) or environment variables
    // (CI secrets). Without a keystore the release build stays unsigned.
    val keystoreProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    fun signingValue(property: String, env: String): String? =
        keystoreProperties.getProperty(property) ?: System.getenv(env)?.takeIf { it.isNotBlank() }
    val releaseStoreFile = signingValue("storeFile", "TOOTSIE_KEYSTORE_FILE")

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("storePassword", "TOOTSIE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "TOOTSIE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "TOOTSIE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// AGP's lint tool classpath (androidLintTool) pulls in BouncyCastle 1.80.2, affected by
// GHSA-9pwp-9qqc-pr26 and GHSA-qp49-qgx5-5m26. Use the catalog version everywhere until a
// stable AGP ships a fixed version (see also the buildscript block in the root build file).
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on")) {
            useVersion(libs.versions.bouncycastle.get())
            because("BouncyCastle < 1.85 is affected by GHSA-9pwp-9qqc-pr26 and GHSA-qp49-qgx5-5m26")
        }
    }
}

// The lint tool classpath has the same vulnerable transitive dependencies as the build
// classpath (see the root build file); raise them there as well
configurations.matching { it.name == "androidLintTool" }.configureEach {
    listOf(libs.build.commons.lang3, libs.build.httpclient, libs.build.jdom2, libs.build.jose4j).forEach { floor ->
        dependencyConstraints.add(project.dependencies.constraints.create(floor.get()))
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.googlefonts)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    // Required by Dagger/Hilt 2.60+ generated code (CanIgnoreReturnValue)
    compileOnly(libs.errorprone.annotations)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Accompanist
    implementation(libs.accompanist.pager)

    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    // Generates the factories HiltWorkerFactory needs for @HiltWorker classes
    ksp(libs.androidx.hilt.compiler)

    // UnifiedPush
    implementation(libs.unifiedpush.connector)

    // BouncyCastle (VAPID key generation)
    implementation(libs.bouncycastle.bcprov)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.paging.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.work.testing)
}