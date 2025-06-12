plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false // Sobrescribe la versión del catálogo
    id("com.google.gms.google-services") version "4.4.2" apply false
}