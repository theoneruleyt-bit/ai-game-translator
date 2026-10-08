plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.optima.gametranslator"
 compileSdk = 35
 defaultConfig { applicationId = "com.optima.gametranslator"; minSdk = 29; targetSdk = 35; versionCode = 4; versionName = "0.4.0-studio" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}

dependencies {
 implementation("androidx.activity:activity-ktx:1.10.1")
 implementation("androidx.camera:camera-camera2:1.4.2")
 implementation("androidx.camera:camera-lifecycle:1.4.2")
 implementation("androidx.camera:camera-view:1.4.2")
 implementation("com.google.mlkit:text-recognition:16.0.1")
 implementation("com.google.mlkit:translate:17.0.3")
 testImplementation("junit:junit:4.13.2")
}
