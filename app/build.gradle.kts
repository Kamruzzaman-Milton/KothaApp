import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.kotha.dictate"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.shizuku.api)
  implementation(libs.shizuku.provider)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

abstract class SyncPersistentKeystoreTask : DefaultTask() {
  @get:InputFile
  abstract val sourceBase64File: RegularFileProperty

  @get:OutputFile
  abstract val targetKeystoreFile: RegularFileProperty

  @get:Optional
  @get:OutputFile
  abstract val targetRootBase64File: RegularFileProperty

  @TaskAction
  fun sync() {
    val sourceFile = sourceBase64File.get().asFile
    val rawBase64 = sourceFile.readText(Charsets.UTF_8).trim()
    if (rawBase64.isEmpty()) {
      throw org.gradle.api.GradleException("Keystore Base64 source file is empty: ${sourceFile.path}")
    }
    val decodedBytes = Base64.getMimeDecoder().decode(rawBase64)

    val keystoreOut = targetKeystoreFile.get().asFile
    keystoreOut.parentFile?.mkdirs()
    if (!keystoreOut.exists() || !keystoreOut.readBytes().contentEquals(decodedBytes)) {
      keystoreOut.writeBytes(decodedBytes)
    }

    if (targetRootBase64File.isPresent) {
      val normalizedBase64 = rawBase64.replace("\\s+".toRegex(), "") + "\n"
      val rootBase64Out = targetRootBase64File.get().asFile
      rootBase64Out.parentFile?.mkdirs()
      if (!rootBase64Out.exists() || rootBase64Out.readText(Charsets.UTF_8) != normalizedBase64) {
        rootBase64Out.writeText(normalizedBase64, Charsets.UTF_8)
      }
    }
  }
}

val persistentKeystoreBase64 = layout.projectDirectory.file("signing/persistent-debug.keystore.base64")
val rootKeystoreFile = rootProject.layout.projectDirectory.file("debug.keystore")
val rootKeystoreBase64File = rootProject.layout.projectDirectory.file("debug.keystore.base64")

val syncPersistentKeystore = tasks.register<SyncPersistentKeystoreTask>("syncPersistentKeystore") {
  val persistentFile = persistentKeystoreBase64.asFile
  val hasPersistent = persistentFile.exists() && persistentFile.readText(Charsets.UTF_8).isNotBlank()
  if (hasPersistent) {
    sourceBase64File.set(persistentKeystoreBase64)
    targetRootBase64File.set(rootKeystoreBase64File)
  } else {
    sourceBase64File.set(rootKeystoreBase64File)
  }
  targetKeystoreFile.set(rootKeystoreFile)
}

tasks.matching { it.name == "preBuild" || it.name == "validateSigningDebug" || it.name == "packageDebug" }.configureEach {
  dependsOn(syncPersistentKeystore)
}
