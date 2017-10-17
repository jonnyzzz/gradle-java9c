package org.jonnyzzz.gradle.java9c

import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceSet
import java.io.File
import java.util.*
import kotlin.collections.HashSet


@Suppress("DataClassPrivateConstructor")
data class ClasspathEntry
private constructor(
        val name: String
) : Comparable<ClasspathEntry> {

  override fun compareTo(other: ClasspathEntry) = this.name.compareTo(other.name)

  companion object {
    private fun projectName(project: Project) = if (project == project.rootProject)
      "<root>"
    else
      project.path

    fun fromProject(project: Project) = ClasspathEntry("project ${projectName(project)}")

    fun fromDummyPath(file: File) = ClasspathEntry("file: $file")

    //select path with shortest relative path from a project root
    fun fromUnknownPath(project: Project, file : File) : ClasspathEntry = project.rootProject.allprojects
              .asSequence()
              .mapNotNull { p ->
                file.absoluteFile.relativeToOrNull(p.rootDir.absoluteFile)?.let { it to p }
              }.filterNot {
                it.first.startsWith("..")
              }.minBy { it.first.length() }
              ?.let { bestMatch ->
                ClasspathEntry("file: <${projectName(bestMatch.second)}/${bestMatch.first}")
              } ?: fromDummyPath(file)

    fun fromResolvedArtifact(artifact: ResolvedArtifactResult) = ClasspathEntry(artifact.id.displayName)
  }
}

class ResolverContext(
        val project: Project,
        val sourceSet: SourceSet
) {
  val allEntries = TreeSet<File>()
  private val resolvedEntries = TreeMap<File, ClasspathEntry>()

  val rootProject : Project
    get() = project.rootProject

  fun resolveEntry(path: File, entry : ClasspathEntry) {
    if (allEntries.contains(path)) {
      resolvedEntries.put(path, entry)
    }
  }

  fun resolveEntries(e: Iterable<Pair<File, ClasspathEntry>>) = e.forEach { (file, entry) -> resolveEntry(file, entry) }

  val unresolvedEntries
    get() = HashSet(allEntries - resolvedEntries.keys)

  fun complete() : Map<File, ClasspathEntry> {
    for (file in unresolvedEntries) {
      resolveEntry(file, ClasspathEntry.fromDummyPath(file))
    }

    return resolvedEntries.toMap()
  }
}

class Resolver {
  fun fileCollectionResolver(project: Project, sourceSet: SourceSet) : Map<File, ClasspathEntry> {
    val ctx = ResolverContext(project, sourceSet)

    tryAndHandle {
      //setup default set of artifacts to scan
      ctx.allEntries += ctx.sourceSet.runtimeClasspath.toSet().filter { it.exists() }
    }

    tryAndHandle {
      //current source set contains many output directories (e.g. if java, kotlin are used, or resources)
      //merging them together first
      ResolveSourceSetOutput().resolve(ctx)
    }

    tryAndHandle {
      //check incomeing dependencies first
      ResolveConfigurationIncomingDependencies().resolve(ctx)
    }

    tryAndHandle {
      ResolveProjectsFromFilesHack().resolve(ctx)
    }

    //list all files from the classpath and add dummy description to them
    //NOTE: works in |projects| * |classpath entries|
    tryAndHandle {
      ResolveDummyEntries().resolve(ctx)
    }

    return ctx.complete()
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

class ResolveDummyEntries {
  fun resolve(ctx: ResolverContext) = ctx.run {
    resolveEntries(unresolvedEntries
            .filter { it.exists() }
            .map { it to ClasspathEntry.fromUnknownPath(project, it) }
    )
  }
}

class ResolveProjectsFromFilesHack {
  fun resolve(ctx: ResolverContext) = ctx.run {

    for (project in rootProject.allprojects) {
      //TODO: hack is we hardcoded `classes` here

      val classesDir = File(project.buildDir, "classes")
      if (!classesDir.isDirectory) continue

      unresolvedEntries
              .filter { it.absolutePath.startsWith(classesDir.absolutePath) }
              .forEach {
                //assume that path of a project means
                resolveEntry(it, ClasspathEntry.fromProject(project))
              }
    }
  }
}

class ResolveConfigurationIncomingDependencies {
  fun resolve(ctx: ResolverContext) = ctx.run {
    val toResolve = unresolvedEntries

    sequenceOf(
            sourceSet.compileClasspathConfigurationName,
            sourceSet.runtimeClasspathConfigurationName,

            sourceSet.apiConfigurationName,
            sourceSet.implementationConfigurationName
    ).mapNotNull {
      try {
        project.configurations.findByName(it)
      } catch (t: Throwable) {
        Logging.getLogger(javaClass).debug("Failed to resolve configuration $it. ${t.message}", t)
        null
      }
    }
            .flatMap { it.incoming.artifacts.asSequence() }
            .filter { toResolve.contains(it.file) }
            .forEach {
              resolveEntry(
                      it.file,
                      ClasspathEntry.fromResolvedArtifact(it))
            }
  }
}

class ResolveSourceSetOutput {
  fun resolve(ctx: ResolverContext) = ctx.run {
    val projectEntry = ClasspathEntry.fromProject(project)

    sourceSet.output.forEach { dir ->
      resolveEntry(dir, projectEntry)
    }
  }
}
