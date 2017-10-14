package org.jonnyzzz.gradle.java9c

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction


open class GradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    //explicitly include 'java' plugin
    project.plugins.apply(JavaBasePlugin::class.java)

    val task = project.tasks.maybeCreate("java9c", DefaultTask::class.java)

    project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { set ->
      val scanTask = project.tasks.maybeCreate("java9c_${set.name}", ScanClasspathTask::class.java)

      scanTask.classpath = { set.runtimeClasspath }
      scanTask.dependsOn(project.tasks.getByName(set.classesTaskName))

      task.dependsOn(scanTask)
    }
  }
}

open class ScanClasspathTask : DefaultTask() {
  lateinit var classpath: () -> FileCollection?

  @TaskAction
  open fun `execute java9c task`() {
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

}
