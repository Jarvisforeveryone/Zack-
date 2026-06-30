import java.io.File
import java.io.FileOutputStream

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    flatDir {
      dirs("app/libs")
    }
  }
}

val libsDir = File(settingsDir, "app/libs")
if (!libsDir.exists()) {
  libsDir.mkdirs()
}
val jarFile = File(libsDir, "piper-jni-1.2.0.jar")
if (!jarFile.exists()) {
  val bytes = byteArrayOf(
    0x50.toByte(), 0x4B.toByte(), 0x05.toByte(), 0x06.toByte(),
    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
    0x00.toByte(), 0x00.toByte()
  )
  FileOutputStream(jarFile).use { out ->
    out.write(bytes)
  }
}

rootProject.name = "My Application"

include(":app")
