package org.jonnyzzz.gradle.java9c

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class IntegrationTest {

  val gradleVersion = "4.2.1"

  private val pluginArtifact by lazy {
    val v = System.getProperty("jonnyzzz-plugin-artifact") ?: error("Failed to get version. Are you running via Gradle test task?")
    println("Plugin version: $v")
    v
  }

  @Rule
  @JvmField
  val temp = TemporaryFolder()

  fun toProject(builder: GradleBuilder.() -> Unit): GradleRunner {
    val projectDir = temp.newFolder()

    fileWriter(projectDir, "build.gradle") {
      -"buildscript {"
      -"  repositories {"
      -"    mavenLocal() "
      -"    mavenCentral() "
      -"  }"
      -""
      -"  dependencies {"
      -"    classpath '$pluginArtifact'"
      -"  }"
      -""
      -"}"
      -""
      -""
      -"println(System.getProperty(\"java.version\"))\n "
      -"apply plugin: 'org.jonnyzzz.java9c'"
      -""
      -""

      object : GradleBuilder, FileBuilder by this {}.builder()
    }

    return GradleRunner.create()
            .withProjectDir(projectDir)
            .withGradleVersion(gradleVersion)
  }

  @Test
  fun test_smoke() {
    toProject {
    }
            .withArguments("java9c")
            .forwardOutput()
            .build()
  }

  @Test
  fun test_java_clash_sources() {
    toProject {
      fileWriter("settings.gradle") {
        -"include 'a'"
        -"include 'b'"
      }

      fileWriter("a/src/main/java/org/foo/x.kt") {
        -"package org.foo"
        -""
        -"class X"
      }

      fileWriter("b/src/main/java/org/foo/y.kt") {
        -"package org.foo"
        -""
        -"class Y"
      }

      -"allprojects { apply plugin: 'java' } "
      -"dependencies { compile project(':a'), project(':b') }"
    }.withArguments("java9c").forwardOutput().build()
  }
}
