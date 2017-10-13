package org.jonnyzzz.gradle.java9c

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

interface FileBuilder {
  operator fun String.unaryMinus()

  fun fileWriter(name: String, builder: FileBuilder.() -> Unit)
}

interface GradleBuilder : FileBuilder

private fun fileWriter(dir: File, name: String, builder: FileBuilder.() -> Unit) {
  val text = buildString {

    object : FileBuilder {
      override fun String.unaryMinus() {
        appendln(this)
      }

      override fun fileWriter(name: String, builder: FileBuilder.() -> Unit) {
        fileWriter(dir, name, builder)
      }

    }.builder()
  }

  File(dir, name).apply {
    println("File $this: ")
    println(text)
    println()
    parentFile.mkdirs()
    writeText(text)
  }
}

class IntegrationTest {

  val gradleVersion = "4.2.1"

  private val pluginVersion by lazy {
    val v = System.getProperty("jonnyzzz-plugin-version") ?: error("Failed to get version. Are you running via Gradle test task?")
    println("Plugin version: $v")
    v
  }

  private val pluginClasspath by lazy {
    val cp = System.getProperty("jonnyzzz-plugin-classpath") ?: error("Failed to get classpath. Are you running via Gradle test task?")
    cp.split(File.pathSeparatorChar).map { File(it) }.also {
      println("Plugin classpath: $it")
    }
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
      -"    classpath 'org.jonnyzzz:java9c:$pluginVersion'"
      -"  }"
      -""
      -"}"
      -"println(System.getProperty(\"java.version\"))\n "
      -"apply plugin: 'org.jonnyzzz.java9c'"
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

       /*
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
    }.withArguments("java9c", "--debug").withDebug(true).forwardOutput().build()
  }
*/
}
