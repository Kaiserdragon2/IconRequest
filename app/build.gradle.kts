plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.kaiserdragon.iconrequest"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "de.kaiserdragon.iconrequest"
        minSdk = 24
        targetSdk = 36
        versionCode = 32
        versionName = "2.8.0"
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug{
            applicationIdSuffix = ".debug"
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
base {
        archivesName = "IconRequest-v${android.defaultConfig.versionName}"
}
composeCompiler {
    // This stops the task that is looking for the missing mapping artifact
    includeComposeMappingFile.set(false)
}
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val versionName = android.defaultConfig.versionName
        val apkName = "IconRequest-v$versionName.apk"

        // Create a custom task to copy and rename the output
        tasks.register<Copy>("renameReleaseApk") {
            // Get the actual APK produced by the build
            from(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK))

            // Set the destination (the standard output folder)
            into(layout.buildDirectory.dir("outputs/apk/release"))

            // Apply the rename
            rename { apkName }

            // Ensure we don't copy the metadata JSON, just the APK
            include("*.apk")
        }

        // Link this task to the assembleRelease task
        tasks.named("assemble").configure {
            finalizedBy("renameReleaseApk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.adaptive.icon.bitmap)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.palette.ktx)
}