package org.jonnyzzz.gradle.java9c

import org.junit.Before

open class IntegrationTest_4_1 : IntegrationTest_4_2_1() {
  override val gradleVersion = "4.1"

  @Before
  fun ensureJava8Only() {
    assumeJava8()
  }
}
