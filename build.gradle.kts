// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 9.4.1 (APK signing) pulls in BouncyCastle 1.80.2, which is affected by
        // GHSA-9pwp-9qqc-pr26 and GHSA-qp49-qgx5-5m26. Raise it on the build classpath until
        // a stable AGP ships a fixed version.
        classpath(libs.bouncycastle.bcprov)
        classpath(libs.bouncycastle.bcpkix)
        classpath(libs.bouncycastle.bcutil)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}