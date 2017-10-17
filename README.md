[![Build Status](https://travis-ci.org/jonnyzzz/gradle-java9c.svg?branch=master)](https://travis-ci.org/jonnyzzz/gradle-java9c)

Gradle Java9 Packages Collision plugin
======================================

Starting from Java9 it is no longer allowed to have same 
packages in different modules (jars) 

The plugin helps to detect package name collisions, that blocks one from 
migrating to Java 9 modules (JIGSAW)


License
=======

Apache 2.0. See LICENSE.txt file in the repo for details


Usage
=====

The plugin defines `java9c` task for the project. The task generates detected
packages collisions for every SourceSet of the project.

Add the following lines to your project:
```gradle

plugins {
  id "org.jonnyzzz.java9c" version "<USE_LATEST_VERSION>"
}


//necessary only for multiple project projects:
subprojects {
  apply plugin: "org.jonnyzzz.java9c"
}

```


Issues
======

Please use GitHub issues from the project to report problems you have. Pull requests are welcome too.



