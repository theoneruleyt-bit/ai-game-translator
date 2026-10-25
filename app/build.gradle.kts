import java.util.Properties
import java.net.URI
import java.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val playConfig = Properties().apply {
    val file = rootProject.file("release.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun config(name: String) =
    providers.environmentVariable(name).orNull
        ?: playConfig.getProperty(name, "")

fun resourceXml(value: String) = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace("\"", "\\\"")
    .replace("'", "\\'")

android {
    namespace = "com.optima.gametranslator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.optima.gametranslator"
        minSdk = 29
        targetSdk = 36
        versionCode = 47
        versionName = "0.31.4-server-pilot"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("signing/optima-test.jks")
            storePassword = "android"
            keyAlias = "optima-test"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false

            if (config("PLAY_UPLOAD_STORE_FILE").isNotBlank()) {
                signingConfig = signingConfigs.create("playUpload") {
                    storeFile = rootProject.file(
                        config("PLAY_UPLOAD_STORE_FILE")
                    )
                    storePassword = config("PLAY_UPLOAD_STORE_PASSWORD")
                    keyAlias = config("PLAY_UPLOAD_KEY_ALIAS")
                    keyPassword = config("PLAY_UPLOAD_KEY_PASSWORD")
                }
            }
        }
    }

    sourceSets.getByName("main").res.srcDir(
        layout.buildDirectory.dir("generated/playRelease/res")
    )

    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")
    implementation("com.google.ai.edge.litert:litert:1.4.1")
    implementation(files("libs/sherpa-onnx-static-1.13.8.aar"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

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

val generatePlayConfig by tasks.registering {
    val values = mapOf(
        "play_license_key" to config("PLAY_LICENSE_KEY"),
        "privacy_policy_url" to config("PLAY_PRIVACY_URL"),
        "support_email" to config("PLAY_SUPPORT_EMAIL")
    )

    inputs.properties(values)

    val directory = layout.buildDirectory.dir("generated/playRelease/res")
    outputs.dir(directory)

    doLast {
        val file = directory.get().file("values/play-config.xml").asFile
        file.parentFile.mkdirs()

        file.writeText(
            "<resources>\n" +
                values.entries.joinToString("\n") { (key, value) ->
                    "<string name=\"$key\" translatable=\"false\" " +
                        "formatted=\"false\">${resourceXml(value)}</string>"
                } +
                "\n</resources>"
        )
    }
}

tasks.named("preBuild").configure {
    dependsOn(generatePlayConfig)
}

val verifyPlayConfiguration by tasks.registering {
    group = "verification"
    description =
        "Validate store-specific values and private upload signing before producing a submission bundle"

    doLast {
        val email = config("PLAY_SUPPORT_EMAIL")

        require(
            Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email)
        ) {
            "Set PLAY_SUPPORT_EMAIL in private release.properties"
        }

        val uri = URI(config("PLAY_PRIVACY_URL"))

        require(
            uri.scheme == "https" &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                !uri.host.endsWith("example.com")
        ) {
            "Set the published HTTPS PLAY_PRIVACY_URL"
        }

        val bytes = Base64.getMimeDecoder().decode(
            config("PLAY_LICENSE_KEY")
        )

        KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(bytes)
        )

        val store = rootProject.file(config("PLAY_UPLOAD_STORE_FILE"))

        require(
            config("PLAY_UPLOAD_STORE_FILE").isNotBlank() &&
                store.isFile &&
                store.canonicalPath !=
                rootProject.file("signing/optima-test.jks").canonicalPath
        ) {
            "Configure a private Play upload key; never use the public debug key"
        }

        require(
            listOf(
                "PLAY_UPLOAD_STORE_PASSWORD",
                "PLAY_UPLOAD_KEY_ALIAS",
                "PLAY_UPLOAD_KEY_PASSWORD"
            ).all { config(it).isNotBlank() }
        ) {
            "Set upload signing values privately"
        }

        val keys = KeyStore.getInstance("JKS")

        store.inputStream().use {
            keys.load(
                it,
                config("PLAY_UPLOAD_STORE_PASSWORD").toCharArray()
            )
        }

        val alias = config("PLAY_UPLOAD_KEY_ALIAS")

        val certificate = requireNotNull(keys.getCertificate(alias)) {
            "Upload certificate alias not found"
        }

        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

        require(
            fingerprint !=
                "ad6cf7228e75cb0a702f03aca74fa825a79a8a447ce8f3118a064b46364f6f1d"
        ) {
            "The public test key is not a production upload key, even if renamed"
        }

        require(
            keys.getKey(
                alias,
                config("PLAY_UPLOAD_KEY_PASSWORD").toCharArray()
            ) != null
        ) {
            "Upload private key cannot be read"
        }
    }
}

tasks.register("playSubmissionBundle") {
    group = "build"
    dependsOn(verifyPlayConfiguration, "bundleRelease")
}

tasks.matching {
    it.name == "bundleRelease"
}.configureEach {
    mustRunAfter(verifyPlayConfiguration)
}
