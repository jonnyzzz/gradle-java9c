package org.jonnyzzz.gradle.java9c

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class PluginTest {
  @Rule
  @JvmField
  val temp = TemporaryFolder()
  
  private fun project(builder: GradleBuilder.() -> Unit = {}): Project {
    val projectDir = temp.newFolder()

    fileWriter(projectDir, "build.gradle") {
      -""
      -"println(System.getProperty(\"java.version\"))\n "
      -""

      object : GradleBuilder, FileBuilder by this {}.builder()
    }

    return ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()
            .also {
              it.pluginManager.apply(GradlePlugin::class.java)
            }
  }

  @Test
  fun hasTask() {
    project().tasks.getByName("java9c")
  }
}
