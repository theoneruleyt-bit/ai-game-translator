plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.optima.gametranslator"
 compileSdk = 35
 defaultConfig { applicationId = "com.optima.gametranslator"; minSdk = 29; targetSdk = 35; versionCode = 9; versionName = "0.9.0-streaming-speech" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 packaging { resources { excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/io.netty.versions.properties") } }
}

dependencies {
 implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.52.0")
 implementation("androidx.activity:activity-ktx:1.10.1")
 implementation("androidx.camera:camera-camera2:1.4.2")
 implementation("androidx.camera:camera-lifecycle:1.4.2")
 implementation("androidx.camera:camera-view:1.4.2")
 implementation("com.google.mlkit:text-recognition:16.0.1")
 implementation("com.google.mlkit:translate:17.0.3")
 testImplementation("junit:junit:4.13.2")
}
