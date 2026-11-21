import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

val requestedTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val releaseRequested = requestedTasks.any {
    it.contains("Release", ignoreCase = true) || it == "assemble" || it == "build"
}
if (releaseRequested) {
    check(keystorePropertiesFile.isFile) {
        "Falta keystore.properties. Copia keystore.properties.example y configura la firma release."
    }
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").forEach { key ->
        check(!keystoreProperties.getProperty(key).isNullOrBlank()) { "Falta $key en keystore.properties." }
    }
    val store = rootProject.file(keystoreProperties.getProperty("storeFile"))
    check(store.isFile) { "No existe el archivo de firma configurado: $store" }
}

android {
    namespace = "cl.duoc.coegio_pablo_saladana"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cl.duoc.coegio_pablo_saladana"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Configuración de conexión (Tarea 5). Sobrescribir sin tocar código:
        //   Emulador (por defecto):  http://10.0.2.2:8090/  (puerto real del backend: server.port=8090)
        //   Dispositivo físico:      ./gradlew assembleDebug -PapiBaseUrl=http://192.168.1.50:8090/
        //   Servidor externo:        ./gradlew assembleRelease -PapiBaseUrl=https://api.example.org/
        // La integración está activa para el backend local; SchoolBackend rechaza HTTP fuera de hosts privados.
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "http://10.0.2.2:8090/"
        val remoteSyncEnabled =
            (project.findProperty("remoteSyncEnabled") as String?)?.toBooleanStrictOrNull() ?: true
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "REMOTE_SYNC_ENABLED", remoteSyncEnabled.toString())
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                val storePath = keystoreProperties.getProperty("storeFile")
                if (!storePath.isNullOrBlank()) {
                    storeFile = rootProject.file(storePath)
                }
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// La pauta mide lógica: excluye UI Compose, Android I/O y código generado por Room.
val logicCoverageData = layout.buildDirectory.file(
    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
)
val logicClassDirectories = fileTree(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
) {
    include(
        "**/domain/model/**",
        "**/domain/validation/**",
        "**/domain/sync/**",
        "**/domain/error/**",
        "**/data/remote/dto/**",
        "**/ui/navigation/**",
        "**/ui/auth/AuthUiState*",
        "**/ui/auth/AuthViewModel*"
    )
}

tasks.register<JacocoReport>("logicCoverageReport") {
    group = "verification"
    description = "Cobertura unitaria de lógica portable, sin UI Compose ni código generado por Room."
    dependsOn("testDebugUnitTest")

    executionData(logicCoverageData)
    classDirectories.setFrom(logicClassDirectories)
    sourceDirectories.setFrom(files("src/main/java"))
    reports {
        html.required.set(true)
        xml.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/coverage/logic"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/coverage/logic/report.xml"))
    }
}

val verifyLogicCoverage by tasks.registering(JacocoCoverageVerification::class) {
    group = "verification"
    description = "Exige al menos 80% de cobertura de líneas en la lógica portable."
    dependsOn("testDebugUnitTest")
    executionData(logicCoverageData)
    classDirectories.setFrom(logicClassDirectories)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") { dependsOn(verifyLogicCoverage) }
