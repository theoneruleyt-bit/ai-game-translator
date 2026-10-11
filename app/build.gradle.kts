plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.optima.gametranslator"
 compileSdk = 36
 defaultConfig { applicationId = "com.optima.gametranslator"; minSdk = 29; targetSdk = 36; versionCode = 24; versionName = "0.17-adaptive-audio" }
 signingConfigs { getByName("debug") { storeFile = rootProject.file("signing/optima-test.jks"); storePassword = "android"; keyAlias = "optima-test"; keyPassword = "android" } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 packaging { resources { excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/io.netty.versions.properties") } }
}

dependencies {
 implementation("androidx.credentials:credentials:1.3.0")
 implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
 implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
 implementation("com.android.billingclient:billing:8.3.0")
 implementation("com.alphacephei:vosk-android:0.3.75@aar")
 implementation("net.java.dev.jna:jna:5.18.1@aar")
 implementation("androidx.activity:activity-ktx:1.10.1")
 implementation("androidx.camera:camera-camera2:1.4.2")
 implementation("androidx.camera:camera-lifecycle:1.4.2")
 implementation("androidx.camera:camera-view:1.4.2")
 implementation("com.google.mlkit:text-recognition:16.0.1")
 implementation("com.google.mlkit:translate:17.0.3")
 testImplementation("junit:junit:4.13.2")
}
