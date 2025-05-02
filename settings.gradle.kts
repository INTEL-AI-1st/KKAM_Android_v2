// settings.gradle.kts (루트)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.2.2"
        kotlin("android")              version "1.9.22"
        kotlin("plugin.serialization") version "1.9.22"
    }

    /* ★ 모든 Kotlin 관련 플러그인을 1.9.22 로 강제 */
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion("1.9.22")
            }
        }
    }
}

/* 프로젝트 전역 저장소 */
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Kkam_backup"
include(":app")
