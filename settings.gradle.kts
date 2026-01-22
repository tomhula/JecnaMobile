rootProject.name = "JecnaMobile"
include(":app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        /* TODO: Update to a release once its made */
        /* https://github.com/google/ksp/issues/2743 */
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        /* TODO: Update to a release once its made */
        /* https://github.com/google/ksp/issues/2743 */
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://jitpack.io")
    }
}
