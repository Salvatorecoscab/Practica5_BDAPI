import java.util.Properties

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.crudfirebaseapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.crudfirebaseapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //load the values from .properties file
        val keystoreFile = project.rootProject.file("local.properties")
        val properties = Properties()
        properties.load(keystoreFile.inputStream())

        //return empty key in case something goes wrong
        val adminPass = properties.getProperty("adminPass") ?: ""
        buildConfigField("String", "adminPass", adminPass)

    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Importar el BoM de Firebase


    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx) // Recomendado

    implementation(libs.firebase.functions.ktx) // Para llamar a Cloud Functions

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // O la versión más reciente

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))

    implementation(libs.material.v190)
    // Añade estas dependencias
    implementation(libs.glide)
    implementation(libs.firebase.storage)
    implementation(libs.circleimageview) // Para imágenes de perfil circulares
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage.v2021)
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.appcheck.ktx) // O la última versión disponible
    implementation(libs.firebase.appcheck.debug)
    // Tus otras dependencias
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi.kotlin.codegen) // Add this line for codegen
    implementation(libs.moshi.kotlin) // Use the latest version

    androidTestImplementation(libs.androidx.espresso.core)
}