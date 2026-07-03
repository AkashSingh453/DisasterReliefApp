plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.protobuf")
}

android {
    namespace = "com.disasterrelief.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.disasterrelief.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Base URL — change to EC2 IP before deploying
        buildConfigField("String", "SYNC_BASE_URL", "\"http://192.168.31.81:8082/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
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
        compose = true
        buildConfig = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.generateKotlin", "true")
    }

    sourceSets {
        getByName("main") {
            java {
                srcDir("build/generated/source/proto/debug/java")
                srcDir("build/generated/source/proto/debug/grpc")
                srcDir("build/generated/source/proto/debug/grpckt")
                srcDir("build/generated/source/proto/release/java")
                srcDir("build/generated/source/proto/release/grpc")
                srcDir("build/generated/source/proto/release/grpckt")
            }
        }
    }
}

// ── Protobuf / gRPC Code Generation ──
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    // ── Protobuf & gRPC (Cloud Sync + Mesh Serialization) ──
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")
    implementation("io.grpc:grpc-okhttp:1.62.2")
    implementation("io.grpc:grpc-protobuf-lite:1.62.2")
    implementation("io.grpc:grpc-stub:1.62.2")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // MediaPipe GenAI Engine for Qwen model
    implementation("com.google.mediapipe:tasks-genai:0.10.14")

    // ── Core Android ──
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ── Jetpack Compose (BOM) ──
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Navigation (Type-Safe) ──
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ── Hilt (Dependency Injection) ──
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Room (Local Database) ──
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Google Nearby Connections (P2P Mesh) ──
    implementation("com.google.android.gms:play-services-nearby:19.3.0")

    // ── Google Play Services Location (GPS) ──
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // ── OSMDroid (Offline Mapping) ──
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // ── OkHttp (used by gRPC-OkHttp transport & logging) ──
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── Kotlinx Serialization (still used for Room internal serialization) ──
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ── Coroutines ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // ── Google Fonts for Compose ──
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.6")

    // ── Testing ──
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Fix for Gradle implicit dependency error between KSP and Protobuf
androidComponents {
    onVariants(selector().all()) { variant ->
        afterEvaluate {
            val variantName = variant.name.replaceFirstChar { it.uppercase() }
            val protoTaskName = "generate${variantName}Proto"
            val kspTaskName = "ksp${variantName}Kotlin"
            
            tasks.findByName(kspTaskName)?.dependsOn(protoTaskName)
        }
    }
}
