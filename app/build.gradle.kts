import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.bigdata"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bigdata"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val mapsKey = localProperties.getProperty("MAPS_ANDROID_KEY") ?: ""
        val routesKey = localProperties.getProperty("ROUTES_API_KEY") ?: ""
        val publicDataKey = localProperties.getProperty("PUBLIC_DATA_API_KEY") ?: ""
        val parkingApiBaseUrl = localProperties.getProperty("PARKING_API_BASE_URL") ?: ""

        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
        buildConfigField("String", "PUBLIC_DATA_API_KEY", "\"$publicDataKey\"")
        buildConfigField("String", "PARKING_API_BASE_URL", "\"$parkingApiBaseUrl\"")
        buildConfigField("String", "MAPS_ANDROID_KEY", "\"$mapsKey\"")
        buildConfigField("String", "ROUTES_API_KEY", "\"$routesKey\"")
        
        // Package and Cert for security headers
        buildConfigField("String", "ANDROID_PACKAGE_NAME", "\"${applicationId}\"")
        // User needs to provide the real SHA-1 here if available, using a placeholder for now
        buildConfigField("String", "ANDROID_CERT_SHA1", "\"\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.maplibre.sdk)
}