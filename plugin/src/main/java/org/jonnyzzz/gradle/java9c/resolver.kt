package org.jonnyzzz.gradle.java9c

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import java.io.File


data class ClasspathEntry(val name: String) : Comparable<Package> {
  override fun compareTo(other: Package) = this.name.compareTo(other.name)

  companion object {
    private fun projectName(project: Project) = if (project == project.rootProject)
      "<root>"
    else
      project.path

    fun fromProject(project: Project) = ClasspathEntry("project: ${projectName(project)}")

    //select path with shortest relative path
    fun fromUnknownPath(project: Project, file : File) : ClasspathEntry = project.rootProject.allprojects
              .asSequence()
              .mapNotNull { p ->
                file.relativeToOrNull(p.rootDir)?.let { it to p }
              }.filterNot {
                it.first.startsWith("..")
              }.minBy { it.first.length() }
              ?.let { bestMatch ->
                ClasspathEntry("file: <${projectName(bestMatch.second)}/${bestMatch.first}")
              } ?: ClasspathEntry("file: $file")
  }
}

class Resolver {
  fun fileCollectionResolver(project : Project, fileSet: FileCollection?) : Map<File, ClasspathEntry> {
    if (fileSet == null) return emptyMap()


    //full list of files... now we try to optimize it
    //every step is in a dedictaded class to handle NoClassDefFound or NoSuchMethod or similar errors
    //there is no public API for that, so we try our best and
    //attempt to make it possible failure proof
    val pathToClasspathEntry = fileSet.files
            .filter { it.exists() }
            .map { it to ClasspathEntry.fromUnknownPath(project, it) }
            .toMap()
            .toMutableMap()

    val ctx = ResolverContext(project.rootProject, fileSet, pathToClasspathEntry)

    tryAndHandle {
      ResolveProjectsFromFilesHack().resolve(ctx)
    }

    return ctx.map
  }

  //this one is intentionally not inline
  private fun tryAndHandle(a : () -> Unit) {
    try {
      a()
    } catch (t : Throwable) {
      Logging.getLogger(javaClass).debug("Failed to complete a resolution try: ${t.message}", t)
    }
  }
}

class ResolverContext(
        val rootProject : Project,
        val fileSet: FileCollection,
        val map : MutableMap<File, ClasspathEntry>
)

class ResolveProjectsFromFilesHack {
  fun resolve(ctx: ResolverContext) = ctx.run {

    for (project in rootProject.allprojects) {
      //TODO: hack is we hardcoded `classes` here

      val classesDir = File(project.buildDir, "classes")
      if (!classesDir.isDirectory) continue

      for (e in map.entries) {
        if (e.key.absolutePath.startsWith(classesDir.absolutePath)) {
          //assume that path of a project means
          e.setValue(ClasspathEntry.fromProject(project))
        }
      }
    }
  }
}
