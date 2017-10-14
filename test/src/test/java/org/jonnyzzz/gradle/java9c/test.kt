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

  fun toProject(builder: BuildGradleWriter.() -> Unit): GradleRunner {
    val projectDir = temp.newFolder()

    fileWriter(projectDir) {
      val fw = this
      gradle("build.gradle") {
        buildScript {
          repositories {
            mavenCentral()
            mavenLocal()
          }

          dependencies {
            classpath(DependencyKind.Text(pluginArtifact))
          }
        }
        -""
        -"println(System.getProperty(\"java.version\"))\n "
        `apply plugin`("org.jonnyzzz.java9c")
        -""
        -""

        object : BuildGradleWriter, FileBuilder by fw, GradleWriter by this {}.builder()
      }
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

      fileWriter("a/src/main/java/org/foo/X.java") {
        -"package org.foo;"
        -""
        -"class X {}"
      }

      fileWriter("b/src/main/java/org/foo/Y.java") {
        -"package org.foo;"
        -""
        -"class Y {}"
      }


      allprojects {
        `apply plugin`("java")
      }

      dependencies {
        compile(project(":a") )
        compile(project(":b") )
      }
    }.withArguments("java9c", "--stacktrace").forwardOutput().build()
  }
}
