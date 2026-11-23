import java.util.Properties

// aquí está activo para su uso, ya que hay código
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
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

        //BKD-01: dirección del API Gateway. Por defecto, el computador visto desde el emulador (10.0.2.2).
        //Para un teléfono por USB: adb reverse tcp:8080 tcp:8080 y en gradle.properties urlBackend=http://localhost:8080/
        val urlBackend = (project.findProperty("urlBackend") as String?) ?: "http://10.0.2.2:8080/"
        buildConfigField("String", "URL_BACKEND", "\"$urlBackend\"")
    }

    //RNF-08: la llave para firmar el APK release se lee de keystore.properties, en la raíz del proyecto.
    //Las contraseñas nunca se suben (.gitignore); el .jks sí, en almacen-claves/. Sin el archivo, el release queda sin firmar
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
        //EP4: cobertura de las pruebas unitarias con JaCoCo.
        //Informe: gradlew createDebugUnitTestCoverageReport -> app/build/reports/coverage/test/debug/index.html
        debug {
            enableUnitTestCoverage = true
        }
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
        buildConfig = true
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
    //API externa (Wikipedia) y backend propio: Retrofit con Gson sobre OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    //MockK: simula dependencias en las pruebas unitarias
    testImplementation(libs.mockk)
    //servidor HTTP de mentira para probar las llamadas reales de Retrofit al backend (rutas, JSON y errores)
    testImplementation(libs.okhttp.mockwebserver)
}

//EP4: cobertura de la LÓGICA (meta 80 %). Deja fuera lo que no se prueba con pruebas unitarias:
//pantallas Compose, tema, navegación, código que genera Room y clases que solo envuelven Android.
//Uso: gradlew coberturaLogica -> app/build/reports/jacoco/coberturaLogica/html/index.html
val excluidosCobertura = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/ui/screens/**", "**/ui/components/**", "**/ui/theme/**", "**/navigation/**",
    "**/MainActivity*", "**/TriskeleduApp*", "**/AppContainer*",
    "**/*_Impl*", "**/data/local/dao/**", "**/data/local/AppDatabase*", "**/data/local/CargaInicial*",
    "**/data/local/Converters*", "**/data/repository/Room*",
    "**/data/location/GpsUbicacionRepository*", "**/data/red/AndroidRedRepository*",
    "**/data/notificaciones/Android*", "**/data/location/ServicioLugaresCercanos*",
    "**/data/sincronizacion/ProgramadorSincronizacion*", "**/data/sincronizacion/SincronizacionWorker*",
    "**/RepositoriosCompartidos*", "**/RepositoriosProgreso*",
    "**/RetrofitWikipediaRepository\$Companion*",
    "**/*ComposableSingletons*", "**/*Factory*"
)

tasks.register<JacocoReport>("coberturaLogica") {
    group = "verification"
    description = "Cobertura de la lógica (ViewModels, reglas, validaciones y repositorios) con JaCoCo"
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(excluidosCobertura) })
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("outputs/unit_test_code_coverage/debugUnitTest/*.exec") })
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}