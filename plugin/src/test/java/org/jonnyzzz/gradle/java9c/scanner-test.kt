package org.jonnyzzz.gradle.java9c

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream


class ScannerTest {

  val junitJar by lazy {
    val files = System.getProperty("jonnyzzz_junit_jar") ?: error("missing parameter from the runner!")
    val junit = files.split(File.pathSeparatorChar).firstOrNull { it.endsWith("junit-4.12.jar") } ?: error("junit jar is missing")
    Assert.assertTrue(File(junit).isFile)

    File(junit)
  }

  @Test
  fun scanJar() {
    val packages = listAppPackagesFromJar(junitJar)

    Assert.assertTrue(packages.isNotEmpty())
    Assert.assertTrue(packages.any { it.name == "org.junit"}  )
    Assert.assertTrue(packages.any { it.name == "junit.framework"}  )
    Assert.assertTrue(packages.any { it.name == "org.junit.internal.matchers"}  )
  }

  @Test
  fun scanJUnitIntegration() {
    val jarPackages = listAppPackagesFromJar(junitJar)

    val dir = temp.newFolder()
    JarInputStream(junitJar.inputStream()).use { jar ->
      while(true) {
        val je = jar.nextJarEntry ?: break
        if (je.isDirectory) continue
        
        File(dir, je.name).apply {
          parentFile?.mkdirs()
          outputStream().use { fs ->
            jar.copyTo(fs)
          }
        }
      }
    }

    val dirPackages = listAppPackagesFromFile(dir)

    Assert.assertEquals(dirPackages, jarPackages)
  }

  @Test
  fun scanJarChecksRealPackagesOnly() {
    val file = temp.newFile("foo.jar")

    JarOutputStream(file.outputStream()).use { jar ->
      jar.putNextEntry(JarEntry("org/jonnyzzz/foo.class"))
      jar.write("COFEBABE_MOCK".toByteArray())

      jar.putNextEntry(JarEntry("org/mock/foo.anything"))
      jar.write("COFEBABE_MOCK".toByteArray())
    }

    val packages = listAppPackagesFromJar(file)
    println(packages)

    Assert.assertEquals("org.jonnyzzz", packages.single().name)
  }

  @Test
  fun scanDirChecksRealPackagesOnly() {
    val file = temp.newFolder("foo.base")

    File(file, "org/jonnyzzz/foo.class").apply {
      parentFile?.mkdirs()
      writeText("COFEBABE_MOCK")
    }

    File(file, "org/mock/foo.anything").apply {
      parentFile?.mkdirs()
      writeText("COFEBABE_MOCK")
    }

    val packages = listAppPackagesFromFile(file)
    println(packages)

    Assert.assertEquals("org.jonnyzzz", packages.single().name)
  }

  @Rule
  @JvmField
  val temp = TemporaryFolder()

}