import java.util.Properties
import java.security.KeyStore
import java.security.cert.X509Certificate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseSecrets = Properties().apply {
    val file = rootProject.file(".signing/release.properties")
    if (file.isFile) file.inputStream().use(::load)
}
fun releaseSecret(name: String): String? = providers.environmentVariable("ARCHIMAN_" + name.uppercase()).orNull
    ?: releaseSecrets.getProperty(name)
val releaseKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasReleaseSigning = releaseKeys.all { !releaseSecret(it).isNullOrBlank() }
require(releaseKeys.none { !releaseSecret(it).isNullOrBlank() } || hasReleaseSigning) { "Incomplete release signing configuration; provide all four ARCHIMAN signing values or .signing/release.properties." }

android {
    namespace = "com.holagundi.archiman"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.holagundi.archiman"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.2.0"
        testInstrumentationRunner = "com.holagundi.archiman.BackupDeviceChecks"
    }

    signingConfigs {
        if (hasReleaseSigning) create("production") {
            storeFile = rootProject.file(releaseSecret("storeFile")!!)
            storePassword = releaseSecret("storePassword")
            keyAlias = releaseSecret("keyAlias")
            keyPassword = releaseSecret("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("production")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("audit") {
            initWith(getByName("release"))
            applicationIdSuffix = ".audit"
            versionNameSuffix = "-audit"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    sourceSets.named("main") {
        assets.srcDir(rootProject.file("demo/company"))
    }
    sourceSets.named("androidTest") {
        assets.srcDir(file("schemas"))
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.datastore)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}

tasks.register("writeSecurityInventory") {
    group = "verification"
    description = "Write exact resolved Maven versions for advisory checks."
    doLast {
        val artifacts = configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
        val packages = artifacts.map { artifact ->
            val id = artifact.moduleVersion.id
            "{\"package\":{\"purl\":\"pkg:maven/${id.group}/${id.name}@${id.version}\"}}"
        }.distinct().sorted()
        val destination = layout.buildDirectory.file("audit/osv-request.json").get().asFile
        destination.parentFile.mkdirs()
        destination.writeText("{\"queries\":[${packages.joinToString(",")}]}\n")
    }
}

tasks.register("verifyProductionSigning") {
    group = "verification"
    doLast {
        require(hasReleaseSigning) { "Production signing is required. Configure the owner-controlled release key first." }
        require(releaseSecret("keyAlias") != "androiddebugkey") { "The development key cannot sign production builds." }
        val password = releaseSecret("storePassword")!!.toCharArray()
        try {
            val store = KeyStore.getInstance(rootProject.file(releaseSecret("storeFile")!!), password)
            val certificate = store.getCertificate(releaseSecret("keyAlias")) as X509Certificate
            certificate.checkValidity()
            require(!certificate.subjectX500Principal.name.contains("CN=Android Debug", ignoreCase = true)) { "A debug certificate cannot sign production builds." }
        } finally { password.fill('\u0000') }
    }
}
tasks.register("assembleProduction") {
    group = "build"
    description = "Build a minimized APK with mandatory owner-controlled production signing."
    dependsOn("verifyProductionSigning", "assembleRelease")
}
