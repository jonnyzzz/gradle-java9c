package org.jonnyzzz.gradle.java9c

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction


open class Java9cSettings {

}

open class GradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    //explicitly include 'java' plugin
    project.plugins.apply(JavaBasePlugin::class.java)

    val ext = project.extensions.create("java9c", Java9cSettings::class.java)
    val rootTask = project.tasks.create("java9c")

    project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { set ->
      project.tasks.create("java9c_${set.name}", ScanClasspathTask::class.java) { scanTask ->
        scanTask.sourceSet = set
        scanTask.dependsOn(project.tasks.getByName(set.classesTaskName))
        scanTask.ext = ext

        rootTask.dependsOn(scanTask)
      }
    }
  }
}

open class ScanClasspathTask : DefaultTask() {
  @Internal
  @InputFiles
  lateinit var sourceSet: SourceSet

  @Internal
  lateinit var ext: Java9cSettings

  @TaskAction
  open fun `execute java9c task`() {
    val fileSet = sourceSet.runtimeClasspath

    val packagesToEntries = scanPackages(project, fileSet)
    if (logger.isDebugEnabled) {
      packagesToEntries.toSortedMap().forEach { (k,vs) ->
        vs.forEach { v ->
          logger.debug("${k.name} -> ${v.name}")
        }
      }
    }

    val problems = packagesToEntries.filter { it.value.size > 1 }.toSortedMap()
    if (problems.isEmpty()) {
      logger.debug("All packages usages are unique. No problems detected")
      return
    }

    logger.error("The following packages are defined in several modules:")
    for ((k,vs) in problems) {
      logger.error("Package: ${k.name} is declared in")
      for (v in vs) {
        logger.error("  module: ${v.name}")
      }
      logger.error("  ")
    }
    logger.error("  ")

    //TODO: what is the best way to report error?
    throw Exception("Package collisions were detected")
  }
}
