package org.jonnyzzz.gradle.java9c

import java.util.*

fun report(problems: SortedMap<Package, SortedSet<ClasspathEntry>>,
           print: (String) -> Unit) {

  print("  ")
  print("The following packages are defined in several modules:")
  for ((k, vs) in problems) {
    print("  ")
    print("Package '${k.name}' is declared in")
    for (v in vs.toSortedSet()) {
      print("  - ${v.name}")
    }
    print("  ")
  }
  print("  ")
}


