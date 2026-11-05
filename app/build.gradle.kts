import java.util.Properties

// aquí está activo para su uso, ya que hay código
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cookingplastic.triskeledu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cookingplastic.triskeledu"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    //RNF-08: la llave para firmar el APK release se lee de keystore.properties, en la raíz del proyecto.
    //Ese archivo y la llave (.jks) están en .gitignore: nunca se suben. Sin el archivo, el release queda sin firmar
    val archivoFirma = rootProject.file("keystore.properties")
    val firma = Properties().apply { if (archivoFirma.exists()) archivoFirma.inputStream().use { load(it) } }
    if (!firma.isEmpty) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (!firma.isEmpty) signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    //para probar ViewModels: reemplaza el hilo principal de Android en las pruebas
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.osmdroid.android)

    // la diferencia clave contra ksp es que implementation se aplica mientras se ejecuta en el teléfono. El otro es en compilación
    // ViewModel y estado en Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room: el compiler lo procesa KSP
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // GPS, cámara y lectura de QR
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    //SIM-03: la sincronización simulada corre con WorkManager al volver la red
    implementation(libs.androidx.work.runtime.ktx)

    // Generar QR y mostrar imágenes
    implementation(libs.zxing.core)
    implementation(libs.coil.compose)
    //Media3 (ExoPlayer): reproductor de las cápsulas (P12)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
}