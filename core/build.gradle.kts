plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.github.windsekirun.musicwidget.core"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette)
    testImplementation(libs.junit)
}
