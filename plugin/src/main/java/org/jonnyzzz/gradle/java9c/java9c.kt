package org.jonnyzzz.gradle.java9c

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.streams.asSequence


open class GradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    //explicitly include 'java' plugin
    project.plugins.apply(JavaBasePlugin::class.java)

    val task = project.tasks.maybeCreate("java9c", DefaultTask::class.java)

    project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { set ->
      val scanTask = project.tasks.maybeCreate("java9c_${set.name}", ScanClasspathTask::class.java)

      scanTask.classpath = { set.runtimeClasspath }
      scanTask.dependsOn(project.configurations.getByName(set.runtimeClasspathConfigurationName))

      task.dependsOn(scanTask)
    }
  }
}

open class ScanClasspathTask : DefaultTask() {
  lateinit var classpath: () -> FileCollection?

  @TaskAction
  open fun `execute java9c task`() {
    println("project: " + project.name)

    val fileSet: FileCollection = classpath() ?: return
    println("classpath: " + fileSet.toSet())

    val packagesToEntries =
            fileSet.toSet()
                    .flatMap { file ->
                      (listAppPackagesFromFile(file) + listAppPackagesFromJar(file)).map { it to file }
                    }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                    .filter { it.value.size > 1 }


    packagesToEntries
            .forEach {
              println("Conflicts: ${it.key} -> ${it.value}")
            }
  }

  private fun listAppPackagesFromFile(root: File): Set<String> {
    if (!root.isDirectory) return emptySet()

    val rootPath = root.toPath()
    return Files.walk(rootPath)
            .asSequence()
            .filter { it.fileName.endsWith(".class") }
            .map { rootPath.relativize(it.parent).toFile().path }
            .map { it.replace('/', '.') }
            .toSortedSet()
  }

  private fun listAppPackagesFromJar(root: File): Set<String> {
    if (!root.isFile) return emptySet()

    return JarFile(root).use { jar ->
      jar.entries().asSequence()
              .filter { !it.isDirectory }
              .filter { it.name.endsWith(".class") }
              .map { it.name.substring(0, it.name.lastIndexOf('/')) }
              .map { it.replace('/', '.') }
              .toSortedSet()
    }
  }
}
