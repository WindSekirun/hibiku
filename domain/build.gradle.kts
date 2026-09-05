plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.windsekirun.hibiku.domain"
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
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
