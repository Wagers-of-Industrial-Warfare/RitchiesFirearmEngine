import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.expand
import org.gradle.kotlin.dsl.maven
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.*

val Project.env: Env get() = Env(this)
fun Project.prop(key: String): String = requireNotNull(findProperty(key)?.toString()) { "Missing '$key'" }
fun String.upperCaseFirst() = replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

fun RepositoryHandler.strictMaven(url: String, vararg groups: String) = exclusiveContent {
    forRepository { maven(url) }
    filter { groups.forEach(::includeGroup) }
}

fun ProcessResources.properties(files: Iterable<String>, vararg properties: Pair<String, Any>) {
    for ((name, value) in properties) inputs.property(name, value)
    filesMatching(files) {
        expand(properties.toMap())
    }
}

@JvmInline
value class Env(private val project: Project) {
    fun ci() = System.getenv("CI")?.toBoolean() ?: false
    fun release() = System.getenv("RELEASE")?.toBoolean() ?: false
    fun nightly() = ci() && !release()
    fun buildNumber() = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
}
