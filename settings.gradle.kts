rootProject.name = "JecnaMobile"
include(":app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        /* For com.chrynan.parcelable library */
        maven { url = uri("https://repo.repsy.io/mvn/chrynan/public") }
    }
}
