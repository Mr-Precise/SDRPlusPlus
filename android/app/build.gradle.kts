import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val sdrKitRootPath: String = run {
    if (project.hasProperty("sdrKitRoot")) {
        project.property("sdrKitRoot").toString()
    } else {
        val localProps = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { load(it) }
            }
        }
        val propVal = localProps.getProperty("sdrKitRoot")
        if (!propVal.isNullOrBlank()) {
            propVal
        } else {
            "/sdr-kit"
        }
    }
}

val ndkVersionConfigured: String? = run {
    val localProps = Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { load(it) }
        }
    }
    localProps.getProperty("ndk.version") ?: System.getenv("ANDROID_NDK_VERSION") ?: "26.1.10909125"
}

val targetAbis: List<String> = run {
    val prop = project.findProperty("abiFilters") as? String
    if (!prop.isNullOrBlank()) {
        prop.split(",").map { it.trim() }
    } else {
        val localProps = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { load(it) }
            }
        }
        val localAbis = localProps.getProperty("abiFilters")
        if (!localAbis.isNullOrBlank()) {
            localAbis.split(",").map { it.trim() }
        } else {
            listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }
}

android {
    namespace = "org.sdrpp.sdrpp"
    compileSdk = 35

    ndkVersionConfigured?.let {
        ndkVersion = it
    }

    defaultConfig {
        applicationId = "org.sdrpp.sdrpp"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.2.1"

        ndk {
            abiFilters.addAll(targetAbis)
        }

        externalNativeBuild {
            cmake {
                arguments(
                    "-DOPT_BACKEND_GLFW=OFF",
                    "-DOPT_BACKEND_ANDROID=ON",
                    "-DOPT_BUILD_SOAPY_SOURCE=OFF",
                    "-DOPT_BUILD_ANDROID_AUDIO_SINK=ON",
                    "-DOPT_BUILD_AUDIO_SINK=OFF",
                    "-DOPT_BUILD_DISCORD_PRESENCE=OFF",
                    "-DOPT_BUILD_M17_DECODER=ON",
                    "-DOPT_BUILD_PLUTOSDR_SOURCE=ON",
                    "-DOPT_BUILD_AUDIO_SOURCE=OFF",
                    "-DOPT_BUILD_HYDRASDR_SOURCE=ON",
                    "-DSDR_KIT_ROOT=$sdrKitRootPath"
                )
            }
        }
    }

    signingConfigs {
        create("customDebug") {
            val keystoreFile = file("debug.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            val keystoreFile = file("debug.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("customDebug")
            }
        }
        debug {
            val keystoreFile = file("debug.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("customDebug")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../../CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            // Directly expose resources from root/ directory into app assets
            assets.srcDirs("../../root")
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
        jniLibs {
            useLegacyPackaging = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*targetAbis.toTypedArray())
            isUniversalApk = true
        }
    }
}

androidComponents {
    onVariants { variant ->
        val baseCode = android.defaultConfig.versionCode ?: 1
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }?.identifier
            val abiOffset = when (abi) {
                "armeabi-v7a" -> 1
                "arm64-v8a" -> 2
                "x86" -> 3
                "x86_64" -> 4
                else -> 0 // universal apk
            }
            output.versionCode.set(baseCode * 10 + abiOffset)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
