package mihon.gradle

import org.gradle.api.Project
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround
fun Project.getLatestCommitCount(): String {
    return exec("git rev-list --count HEAD") ?: "0"
}

fun Project.getLatestCommitSha(): String {
    return exec("git rev-parse --short HEAD") ?: "unknown"
}

/**
 * @param useLatestCommitTime If `true`, the build time is based on the timestamp of the last Git commit;
 *                          otherwise, the current time is used. Both are in UTC.
 * @return An ISO 8601 formatted string representing the build time.
 */
fun Project.getBuildTime(useLatestCommitTime: Boolean): String {
    if (useLatestCommitTime) {
        val epoch = exec("git log -1 --format=%ct")?.toLongOrNull()
        if (epoch != null) {
            return Instant.fromEpochSeconds(epoch).toString()
        }
    }

    val now = Clock.System.now()
    return (now - now.nanosecondsOfSecond.nanoseconds).toString()
}

fun Project.exec(command: String): String? {
    return try {
        providers.exec {
            commandLine = command.split(" ")
            isIgnoreExitValue = true
        }
            .standardOutput
            .asText
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }
}
