import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.findByType<LibraryExtension>()
                ?: extensions.findByType<ApplicationExtension>()
                ?: error("AndroidComposeConventionPlugin requires an Android plugin to be applied")

            configureAndroidCompose(extension)
        }
    }
}
