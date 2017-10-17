package org.jonnyzzz.gradle.java9c

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.jonnyzzz.gradle.java9c.DependencyKind.Text
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


open class IntegrationTest_4_2_1 {

  open val gradleVersion = "4.2.1"

  private val pluginArtifact by lazy {
    val v = System.getProperty("jonnyzzz-plugin-artifact") ?: error("Failed to get version. Are you running via Gradle test task?")
    println("Plugin version: $v")
    v
  }

  @Rule
  @JvmField
  val temp = TemporaryFolder()

  fun toProject(header: GradleWriter.() -> Unit = {},
                buildScriptBuilder: GradleBuildScript.() -> Unit = {},
                builder: BuildGradleWriter.() -> Unit): GradleRunner {
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
            classpath(Text(pluginArtifact))
          }

          buildScriptBuilder()
        }

        header()

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

      java9c{ noFail() }
    }.withArguments("java9c", "--stacktrace").forwardOutput().build().assertContains {
      -"Package 'org.foo' is declared in"
      -"  - a.jar (project :a)"
      -"  - b.jar (project :b)"
    }
  }

  @Test
  fun test_javalibrary_clash_sources() {
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
        `apply plugin`("java-library")
      }

      dependencies {
        api(project(":a") )
        implementation(project(":b") )
      }

      java9c { noFail() }

    }.withArguments("java9c").withDebug(true).forwardOutput().build().assertContains {
      -"Package 'org.foo' is declared in"
      -"  - a.jar (project :a)"
      -"  - b.jar (project :b)"
    }
  }

  @Test
  fun test_java_clash_sources_lib() {
    toProject {
      fileWriter("src/main/java/org/junit/X.java") {
        -"package org.junit;"
        -""
        -"class X {}"
      }

      `apply plugin`("java")

      repositories {
        mavenCentral()
      }

      dependencies {
        compile(Text("junit:junit:4.12"))
      }

      java9c { noFail() }
      
    }.withArguments("java9c", "--stacktrace", "--info").forwardOutput().build()
            .assertContains {
                      -"Package 'org.junit' is declared in"
                      -"  - junit.jar (junit:junit:4.12)"
                      -"  - project <root>"
            }
  }

  @Test
  fun test_java_clash_sources_runtimeOnly_lib() {
    toProject {
      fileWriter("src/main/java/org/junit/X.java") {
        -"package org.junit;"
        -""
        -"class X {}"
      }

      `apply plugin`("java")

      repositories {
        mavenCentral()
      }

      dependencies {
        runtimeOnly(Text("junit:junit:4.12"))
      }

      java9c { noFail() }

    }.withArguments("java9c", "--stacktrace", "--info").forwardOutput().build()
            .assertContains {
                      -"Package 'org.junit' is declared in"
                      -"  - junit.jar (junit:junit:4.12)"
                      -"  - project <root>"
            }
  }

  @Test
  fun test_java_clash_sources_compileOnly_lib() {
    toProject {
      fileWriter("src/main/java/org/junit/X.java") {
        -"package org.junit;"
        -""
        -"class X {}"
      }

      `apply plugin`("java")

      repositories {
        mavenCentral()
      }

      dependencies {
        compileOnly(Text("junit:junit:4.12"))
      }

    }.withArguments("java9c", "--stacktrace", "--info").forwardOutput().build()
  }

  @Test
  fun test_java_clash_sources_lib_transitive() {
    toProject {
      fileWriter("problem/src/main/java/org/junit/X.java") {
        -"package org.junit;"
        -""
        -"class X {}"
      }

      fileWriter("junit-setup/src/main/java/org/junit/Y.java") {
        -"package org.junit;"
        -""
        -"class Y {}"
      }

      allprojects {
        `apply plugin`("java")
        `apply plugin`("org.jonnyzzz.java9c")

        repositories {
          mavenCentral()
        }
      }


      fileWriter("settings.gradle") {
        -"include ':junit-setup'"
        -"include ':problem'"
      }

      gradle("junit-setup/build.gradle") {
        dependencies {
          compile(Text("junit:junit:4.12"))
        }
      }

      gradle("problem/build.gradle") {
        dependencies {
          compile(DependencyKind.Project(":junit-setup"))
        }

        java9c { noFail() }
      }
      
    }.withArguments(":problem:java9c").withDebug(true).forwardOutput().build()
            .assertContains {
              -"Package 'org.junit' is declared in"
              -"  - junit-setup.jar (project :junit-setup)"
              -"  - junit.jar (junit:junit:4.12)"
              -"  - project :problem"
            }
  }

  private fun BuildGradleWriter.setup_javalibrary_clash_sources_lib() {
    fileWriter("src/main/java/org/junit/X.java") {
      -"package org.junit;"
      -""
      -"class X {}"
    }

    `apply plugin`("java-library")

    repositories {
      mavenCentral()
    }

    dependencies {
      implementation(Text("junit:junit:4.12"))
    }
  }

  @Test
  fun test_javalibrary_clash_sources_lib() {
    toProject {
      setup_javalibrary_clash_sources_lib()

      java9c { noFail() }

    }.withArguments("java9c", "--stacktrace").withDebug(true).forwardOutput().build()
            .assertContains {
              -"Package 'org.junit' is declared in"
              -"  - junit.jar (junit:junit:4.12)"
              -"  - project <root>"
            }
  }

  @Test(expected = Throwable::class)
  fun test_javalibrary_clash_sources_lib_fails() {
    toProject {
      setup_javalibrary_clash_sources_lib()
    }.withArguments("java9c", "--stacktrace").withDebug(true).forwardOutput().build()
  }

  @Test
  fun test_java_kotlin() {
    toProject (header = {
      plugins {
        id("org.jetbrains.kotlin.jvm", "1.1.51")
      }
    }) {
      fileWriter("src/main/java/org/junit/X.java") {
        -"package org.junit;"
        -""
        -"class X {}"
      }

      fileWriter("src/main/kotlin/org/junit/Y.kt") {
        -"package org.junit"
        -""
        -"class Y {}"
      }

      `apply plugin`("java-library")

      repositories {
        mavenCentral()
      }

      dependencies {
        implementation(Text("org.jetbrains.kotlin:kotlin-stdlib:1.1.51"))
      }
    }.withArguments("java9c", "--stacktrace").withDebug(true).forwardOutput().build()
  }

  private fun BuildResult.assertContains(builder : LineWriter.() -> Unit) = apply {
    val lines = output.split(Regex("[\r\n]+")).map { it.trim() }
    val goldLines = mutableListOf<String>()
    val goldLinesText = buildString {
      object : LineWriter {
        override fun String.unaryMinus() {
          goldLines += this.trim()
          appendln(this)
        }
      }.builder()
    }

    if (lines.containsAll(goldLines)) return@apply
    Assert.assertEquals(output, goldLinesText)
  }
}
