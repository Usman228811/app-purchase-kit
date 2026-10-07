plugins {
    id("com.android.library")
//    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.topedge.purchase.kit"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

group = "com.github.Usman228811"
version = "1.1.1"


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.Usman228811"
                artifactId = "app-purchase-kit"
                version = "1.1.1"
            }
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.android.billingclient:billing-ktx:9.1.0")

}