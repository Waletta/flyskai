import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val mapboxAccessToken: String = localProperties.getProperty("MAPBOX_ACCESS_TOKEN")
    ?: (project.findProperty("MAPBOX_ACCESS_TOKEN") as? String)
    ?: System.getenv("MAPBOX_ACCESS_TOKEN")
    ?: ""

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

    extensions.configure<com.android.build.api.dsl.ApplicationExtension> {

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
        compileSdk = 37

        defaultConfig {
            applicationId = "com.example.flyskai"
            minSdk = 26
            targetSdk = 35
            versionCode = 1
            versionName = "1.5"

            resValue("string", "mapbox_access_token", mapboxAccessToken)
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

        buildFeatures {
            compose = true
            viewBinding = true
            resValues = true
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }


    dependencies {
        implementation(libs.androidx.okhttp3)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.compose.navigation)
        implementation(libs.androidx.lifecycle.compose)
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