package org.jonnyzzz.gradle.java9c

import java.io.File

interface LineWriter {
  operator fun String.unaryMinus()
}

interface FileBuilder {
  fun fileWriter(name: String, builder: LineWriter.() -> Unit)
}

fun fileWriter(dir: File, builder: FileBuilder.() -> Unit) {
  object : FileBuilder {
    override fun fileWriter(name: String, builder: LineWriter.() -> Unit) {
      val text = buildString {

        object : LineWriter {
          override fun String.unaryMinus() {
            appendln(this)
          }

        }.builder()
      }

      File(dir, name).apply {
        println("File $this: \n$text\n\n")
        System.out.flush()
        parentFile.mkdirs()
        writeText(text)
      }
    }
  }.builder()
}

fun LineWriter.offset(): LineWriter {
  val that = this
  return object : LineWriter {
    override fun String.unaryMinus() {
      val s = this
      that.apply { -("  " + s) }
    }
  }
}

fun <T: LineWriter> LineWriter.block(name : String, f: (LineWriter) -> T, builder: T.() -> Unit) {
  -"$name {"
  f(offset()).builder()
  -"}"
  -""
}

interface GradleWriter : LineWriter, RepositoriesHolder, DependenciesHolder

fun LineWriter.gradle() : GradleWriter = object: GradleWriter, LineWriter by this {}
fun FileBuilder.gradle(name: String, builder: GradleWriter.() -> Unit) = fileWriter(name, { gradle().apply(builder) })

interface DependenciesHolder : LineWriter
interface DependenciesWriter : LineWriter

sealed class DependencyKind {
  data class Project(val name: String) : DependencyKind() {
    override fun toDependencyString() = "project('${name}')"
  }

  data class Text(val text: String) : DependencyKind() {
    override fun toDependencyString() = "'$text'"
  }

  abstract fun toDependencyString() : String
}

private fun DependenciesWriter._dep(name: String, vararg deps: DependencyKind) {
  for (dep in deps) {
    -"$name ${dep.toDependencyString()}"
  }
}

fun DependenciesWriter.compile(vararg deps: DependencyKind) = _dep("compile", *deps)
fun DependenciesWriter.testCompile(vararg deps: DependencyKind) = _dep("testCompile", *deps)
fun DependenciesWriter.classpath(vararg deps: DependencyKind) = _dep("classpath", *deps)

fun DependenciesHolder.dependencies(builder: DependenciesWriter.() -> Unit) =
        block("dependencies", {
          object : DependenciesWriter, LineWriter by it {
          }
        }, builder)


interface RepositoriesHolder : LineWriter
interface GradleBuildScript : LineWriter, RepositoriesHolder, DependenciesHolder

fun GradleWriter.buildScript(builder: GradleBuildScript.() -> Unit) =
        block("buildscript", {
          object : GradleBuildScript, LineWriter by it {
          }
        }, builder)

interface RepositoriesWriter : LineWriter

fun RepositoriesHolder.repositories(builder: RepositoriesWriter.() -> Unit) =
        block("repositories", {
          object : RepositoriesWriter, LineWriter by it {
          }
        }, builder)

fun RepositoriesWriter.mavenCentral() { -"mavenCentral()" }
fun RepositoriesWriter.mavenLocal() { -"mavenLocal()" }
fun RepositoriesWriter.jcenter() { -"jcenter()" }

fun GradleWriter.allprojects(builder : GradleWriter.() -> Unit) = block("allprojects", {
  object: GradleWriter, LineWriter by it {}
}, builder)

fun GradleWriter.subprojects(builder : GradleWriter.() -> Unit) = block("subprojects", {
  object: GradleWriter, LineWriter by it {}
}, builder)


fun GradleWriter.`apply plugin`(id: String) { -"apply plugin: '${id}'"}

fun GradleWriter.project(name: String) : DependencyKind = DependencyKind.Project(name)

interface BuildGradleWriter : GradleWriter, FileBuilder
