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
  fun `execute java9c task`() {
    println("project: " + project.name)

    val fileSet : FileCollection = classpath() ?: return
    println("classpath: " + fileSet.toSet())

    fileSet.toSet().map {
      it to (listAppPackagesFromFile(it) + listAppPackagesFromJar(it))
    }.map {
      println("Tree: ${it.first} -> ${it.second}")
    }
  }
}

fun listAppPackagesFromFile(root: File): Set<String> {
  if (!root.isDirectory) return emptySet()

  val rootPath = root.toPath()
  return Files.walk(rootPath)
          .asSequence()
          .filter { it.fileName.endsWith(".class") }
          .map { rootPath.relativize(it.parent).toFile().path }
          .map { it.replace('/', '.') }
          .toSortedSet()
}

fun listAppPackagesFromJar(root: File): Set<String> {
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
