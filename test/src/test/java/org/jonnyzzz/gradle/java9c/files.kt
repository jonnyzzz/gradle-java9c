package org.jonnyzzz.gradle.java9c

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
