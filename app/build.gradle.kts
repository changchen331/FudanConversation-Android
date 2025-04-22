import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fudan_conversation.android"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.fudan_conversation.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 读取 local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        // 获取 username
        val username = localProperties.getProperty("API_USERNAME") ?: ""
        buildConfigField("String", "username", "\"$username\"") // 注入到 buildConfig
        // 获取 password
        val password = localProperties.getProperty("API_PASSWORD") ?: ""
        buildConfigField("String", "password", "\"$password\"")
        // 获取 appId
        val appId = localProperties.getProperty("XF_APPID") ?: ""
        buildConfigField("String", "appId", "\"$appId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets {
        getByName("main") {
            jni {
                srcDirs("src\\main\\jni", "src\\main\\jniLibs")
            }
        }
    }

    android.applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                val versionName = versionName
                val versionCode = versionCode
                val buildType = buildType.name
                val createTime =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                this.outputFileName =
                    "复旦问答_v${versionName}.${versionCode}_${buildType}_${createTime}.apk"
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(files("libs/Msc.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.okhttp)
}