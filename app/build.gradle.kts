plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.aivoice"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aivoice"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true  //开启数据绑定

    }
    applicationVariants.all { // 遍历所有应用程序构建变体
        val variant = this // 获取当前变体
        variant.outputs // 遍历当前变体的所有输出文件
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl } // 将输出文件转换为具体类型
            .forEach { output -> // 对每个输出文件进行操作
                val outputFileName = "AiVoice-${variant.baseName}-${variant.versionName}${variant.versionCode}.apk"
                // 自定义输出文件名
                println("OutputFileName: $outputFileName") // 打印输出文件名
                output.outputFileName = outputFileName // 设置输出文件名
            }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.activity.ktx)
    implementation(libs.okhttp)  //网络请求
    implementation(libs.gson)  //缓存
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

}