package org.jonnyzzz.gradle.java9c

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.util.regex.Pattern


open class Java9cSettings {
  /**
   * Should the `java9c` task fail on error or not
   **/
  var failOnCollision = true

  /**
   * Java Pattern to filter sources set names to be checked
   **/
  var sourceSetNameFilter = ".*"
}

open class GradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    //explicitly include 'java' plugin
    project.plugins.apply(JavaBasePlugin::class.java)

    val ext = project.extensions.create("java9c", Java9cSettings::class.java)
    val rootTask = project.tasks.create("java9c")

    val pattern = Pattern.compile(ext.sourceSetNameFilter)

    project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { set ->
      if (!pattern.matcher(set.name).matches()) {
        logger.debug("Source set ${set.name} is ignored by pattern")
        return@all
      }

      val task = project.tasks.create("java9c_${set.name}", ScanClasspathTask::class.java) { scanTask ->
        scanTask.sourceSet = set
        scanTask.dependsOn(project.tasks.getByName(set.classesTaskName))
        scanTask.dependsOn(project.configurations.getByName(set.runtimeConfigurationName))
        scanTask.dependsOn(project.configurations.getByName(set.runtimeClasspathConfigurationName))
        scanTask.ext = ext

        rootTask.dependsOn(scanTask)
      }

      logger.debug("Task ${task.name} was created for the source set ${set.name}")
    }
  }

  private val logger = Logging.getLogger(GradlePlugin::class.java)
}

open class ScanClasspathTask : DefaultTask() {
  @Internal
  @InputFiles
  lateinit var sourceSet: SourceSet

  @Internal
  lateinit var ext: Java9cSettings

  @TaskAction
  open fun `execute java9c task`() {
    val packagesToEntries = scanPackages(project, sourceSet)
    if (logger.isDebugEnabled) {
      packagesToEntries.toSortedMap().forEach { (k, vs) ->
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

    report(problems, {logger.error(it)})

    if (ext.failOnCollision) {
      //TODO: what is the best way to report error?
      throw Exception("Package collisions were detected")
    }
  }
}
