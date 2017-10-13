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
//      scanTask.dependsOn(project.configurations.getByName(set.runtimeClasspathConfigurationName))

      task.dependsOn(scanTask)
    }
  }
}

open class ScanClasspathTask : DefaultTask() {
  lateinit var classpath: () -> FileCollection?

  @TaskAction
  fun `execute java9c task`() {
    println("project: " + project.name)

    println("classpath: " + classpath()?.toSet())

  }
}
