package org.jonnyzzz.gradle.java9c

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskAction


open class GradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    //explicitly include 'java' plugin
    project.plugins.apply(JavaBasePlugin::class.java)

    val task = project.tasks.maybeCreate("java9c", ScanClasspathTask::class.java)

    project.afterEvaluate {
//      TODO: include all source sets here?
//      task.dependsOn(project.tasks.getByName("classes"))
    }
  }
}

open class ScanClasspathTask : DefaultTask() {
  @TaskAction
  fun `execute java9c task`() {
    println(project.name)
  }
}
