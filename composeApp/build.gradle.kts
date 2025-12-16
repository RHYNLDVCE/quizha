import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "1.9.10"
    id("app.cash.sqldelight")
}
val ktorVersion = "3.3.3"
val jwtVersion = "4.5.0"
val serializationVersion = "1.9.0"
val voyagerVersion = "1.0.1"
val cameraVersion = "1.5.2"
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm()
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation("androidx.camera:camera-camera2:$cameraVersion")
            implementation("androidx.camera:camera-lifecycle:$cameraVersion")
            implementation("androidx.camera:camera-view:$cameraVersion")
            implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
            implementation("io.ktor:ktor-client-websockets:${ktorVersion}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            implementation("io.ktor:ktor-client-cio:${ktorVersion}")
            implementation("io.ktor:ktor-client-core:${ktorVersion}")
            // ML Kit Barcode Scanning
            implementation("com.google.mlkit:barcode-scanning:17.3.0")
            implementation("org.slf4j:slf4j-android:1.7.36")

            implementation(compose.materialIconsExtended)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("com.auth0:java-jwt:4.5.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.2.1")
            implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${serializationVersion}")
            implementation("org.mindrot:jbcrypt:0.4")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation("io.ktor:ktor-client-cio:${ktorVersion}")
            implementation("app.cash.sqldelight:coroutines-extensions:2.2.1")
            implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
            implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            implementation("io.ktor:ktor-server-core:${ktorVersion}")
            implementation("io.ktor:ktor-server-netty:${ktorVersion}")
            implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
            implementation("io.ktor:ktor-server-cors:${ktorVersion}")
            implementation("io.ktor:ktor-server-call-logging:${ktorVersion}")
            implementation("io.ktor:ktor-server-status-pages:${ktorVersion}")
            implementation("io.ktor:ktor-server-auth:${ktorVersion}")
            implementation("io.ktor:ktor-server-auth-jwt:${ktorVersion}")
            implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
            implementation("io.ktor:ktor-server-websockets:${ktorVersion}")
            implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("com.auth0:java-jwt:4.5.0")
            implementation("ch.qos.logback:logback-classic:1.5.21")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            // Bcrypt
            implementation("org.mindrot:jbcrypt:0.4")
            implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
            implementation("org.jetbrains.compose.foundation:foundation:1.9.3")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.material.icons.extended)

            implementation("ch.qos.logback:logback-classic:1.5.21")

            //qr
            implementation("com.google.zxing:core:3.5.4")
            implementation("com.google.zxing:javase:3.5.4")
        }
    }
}

android {
    namespace = "com.rai.quizha"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rai.quizha"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.rai.quizha.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.rai.quizha"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    databases {
        create("QuizhaDB") {
            packageName.set("com.rai.quizha.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}