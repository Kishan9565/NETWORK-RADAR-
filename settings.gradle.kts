pluginManagement {
    includeBuild("build-logic")
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NetworkRadar"

include(":app")

include(":core:domain")
include(":core:data")
include(":core:presentation")
include(":core:design-system")
include(":core:database")

include(":feature:dashboard:domain")
include(":feature:dashboard:data")
include(":feature:dashboard:presentation")

include(":feature:radar:domain")
include(":feature:radar:data")
include(":feature:radar:presentation")

include(":feature:speedtest:domain")
include(":feature:speedtest:data")
include(":feature:speedtest:presentation")

include(":feature:map:domain")
include(":feature:map:data")
include(":feature:map:presentation")

include(":feature:heatmap:domain")
include(":feature:heatmap:data")
include(":feature:heatmap:presentation")

include(":feature:history:domain")
include(":feature:history:data")
include(":feature:history:presentation")

include(":feature:comparison:domain")
include(":feature:comparison:data")
include(":feature:comparison:presentation")
