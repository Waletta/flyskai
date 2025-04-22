gradle.projectsEvaluated {
    tasks.matching { it.name.contains("test", ignoreCase = true) }.configureEach {
        enabled = false
    }
}

plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.compose.compiler)
}

    android {

        testOptions {
            unitTests.isReturnDefaultValues = true
        }

        lint {
            checkReleaseBuilds = false
            abortOnError = false
            ignoreWarnings = true
            disable.add("LintBaseline")
            disable.add("MissingTranslation")
            disable.add("UnusedResources")
        }


        namespace = "com.example.flyskai"
        compileSdk = 35

        defaultConfig {
            applicationId = "com.example.flyskai"
            minSdk = 26
            targetSdk = 35
            versionCode = 1
            versionName = "1.5"
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
        }


        buildTypes {
            debug {
                isMinifyEnabled = false
                isDebuggable = true
                // Disable lint for this variant
                enableUnitTestCoverage = false
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        kotlinOptions {
            jvmTarget = "11"
        }
        buildFeatures {
            compose = true
            viewBinding = true
        }
    }


    dependencies {
        implementation(libs.sentencepiece)
        implementation(libs.androidx.okhttp3)
        implementation(libs.onnxruntime.mobile)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation(libs.androidx.navigation.fragment.ktx)
        implementation(libs.androidx.navigation.ui.ktx)
        implementation(libs.play.services.location)
        implementation(libs.mapbox.android)
        implementation(libs.mapbox.extension)
        implementation(libs.mapbox.turf)
        implementation(libs.retrofit)
        implementation(libs.converter.moshi)
        implementation(libs.moshi.kotlin)
        testImplementation(libs.junit)
    }

tasks.configureEach {
    if (name.contains("test", ignoreCase = true)) {
        enabled = false
    }
}