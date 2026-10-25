// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 9.4.1 (APK signing) pulls in BouncyCastle 1.80.2, which is affected by
        // GHSA-9pwp-9qqc-pr26 and GHSA-qp49-qgx5-5m26. Raise it on the build classpath until
        // a stable AGP ships a fixed version.
        classpath(libs.bouncycastle.bcprov)
        classpath(libs.bouncycastle.bcpkix)
        classpath(libs.bouncycastle.bcutil)

        // Vulnerable transitive dependencies of AGP (jose4j CVE-2024-29371, jdom2
        // CVE-2021-33813, commons-lang3 CVE-2025-48924, httpclient CVE-2020-13956).
        // Constraints only raise the version; newer ones requested by AGP still win.
        constraints {
            add("classpath", libs.build.jose4j)
            add("classpath", libs.build.jdom2)
            add("classpath", libs.build.commons.lang3)
            add("classpath", libs.build.httpclient)
        }
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