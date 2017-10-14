package org.jonnyzzz.gradle.java9c

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.streams.asSequence

data class Package(val name : String) : Comparable<Package> {
  override fun compareTo(other: Package) = this.name.compareTo(other.name)
}

data class ClasspathEntry(val name: String) : Comparable<Package> {
  override fun compareTo(other: Package) = this.name.compareTo(other.name)
}

fun listAppPackagesFromFile(root: File): Set<Package> {
  if (!root.isDirectory) return emptySet()

  val rootPath = root.toPath()
  return Files.walk(rootPath)
          .asSequence()
          .filter { it.toFile().path.endsWith(".class") }
          .map { rootPath.relativize(it.parent).toFile().path }
          .map { it.replace('/', '.') }
          .map { Package(it) }
          .toSortedSet()
}

fun listAppPackagesFromJar(root: File): Set<Package> {
  if (!root.isFile) return emptySet()

  return JarFile(root).use { jar ->
    jar.entries().asSequence()
            .filter { !it.isDirectory }
            .filter { it.name.endsWith(".class") }
            .map { it.name.substring(0, it.name.lastIndexOf('/')) }
            .map { it.replace('/', '.') }
            .map { Package(it) }
            .toSortedSet()
  }
}

fun scanPackages(project: Project,
                 fileSet: FileCollection): Map<Package, List<ClasspathEntry>> {

  val projectBuildDir = project.buildDir
  return fileSet.toSet()
          .flatMap { file ->

            //TODO: resolve entries to human readable things
            val entry =
                    if (file.absolutePath.startsWith(projectBuildDir.absolutePath)) {
                      ClasspathEntry("project: ${project.path}")
                    } else {
                      ClasspathEntry(file.path)
                    }

            (listAppPackagesFromFile(file) + listAppPackagesFromJar(file)).map { it to entry }
          }
          .groupBy(keySelector = { it.first }, valueTransform = { it.second })
}
