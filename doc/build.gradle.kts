import com.github.gradle.node.NodeExtension
import com.github.gradle.node.task.NodeSetupTask
import com.github.gradle.node.variant.VariantComputer
import com.github.gradle.node.yarn.task.YarnSetupTask
import com.github.gradle.node.yarn.task.YarnTask
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}
val applicationId: String by rootProject.extra
var versionCode: Int by rootProject.extra
var versionName: String by rootProject.extra
var buildTime: String by rootProject.extra
var buildUser: String by rootProject.extra


val yarnVersionStr: String by rootProject.extra
val nodeVersionStr: String by rootProject.extra
var nodeDistMirror: String by rootProject.extra

node {
    download = true
    version = nodeVersionStr
    yarnVersion = yarnVersionStr
    distBaseUrl = nodeDistMirror
    workDir = file("${project.projectDir}/.gradle/nodejs")
    yarnWorkDir = file("${project.projectDir}/.gradle/yarn")
}

tasks.withType(NodeSetupTask::class.java).configureEach {
    doLast {
        val nodeExtension = NodeExtension[project]
        val variantComputer = VariantComputer()

        val isWindows = nodeExtension.resolvedPlatform.get().isWindows()

        // fix corepack symbolicLink
        fun computeCorepackScriptFile(nodeDirProvider: Provider<Directory>): Provider<String> {
            return nodeDirProvider.map { nodeDir ->
                if (isWindows) nodeDir.dir("node_modules/corepack/dist/corepack.js").asFile.path
                else nodeDir.dir("lib/node_modules/corepack/dist/corepack.js").asFile.path
            }
        }

        val nodeDirProvider = nodeExtension.resolvedNodeDir
        val nodeBinDirProvider = variantComputer.computeNodeBinDir(nodeDirProvider, nodeExtension.resolvedPlatform)
        val nodeBinDirPath = nodeBinDirProvider.get().asFile.toPath()
        val corepackScript = nodeBinDirPath.resolve("corepack")
        val scriptFile =
            computeCorepackScriptFile(nodeDirProvider)
        if (Files.deleteIfExists(corepackScript)) {
            Files.createSymbolicLink(
                corepackScript,
                nodeBinDirPath.relativize(Paths.get(scriptFile.get()))
            )
        }

        val yarnDir = variantComputer.computeYarnDir(nodeExtension).get()
        val dirPath = if (isWindows) yarnDir else yarnDir.dir("bin")

        val nodeExecutable = nodeBinDirPath.resolve("node")
        mkdir(dirPath)
        exec {
            // actually YarnSetup execute here
            commandLine(nodeExecutable, corepackScript, "enable", "--install-directory", dirPath)
        }

    }
}


tasks.withType(YarnSetupTask::class.java).configureEach {
    enabled = false
}

task<YarnTask>("yarnBuild") {
    group = "build"
    dependsOn(tasks.yarn)
    args = listOf("run", "build")
    environment = mapOf("BUILD_VERSION" to versionName, "BUILD_TIME" to buildTime)
}
