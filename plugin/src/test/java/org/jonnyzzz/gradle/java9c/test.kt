package org.jonnyzzz.gradle.java9c

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test


class PluginTest {
  lateinit var project : Project

  @Before
  fun setup() {
    project = ProjectBuilder.builder().build()
    project.plugins.apply("org.jonnyzzz.gradle.java9c")
  }

  @Test
  fun empty() {
    
  }
}