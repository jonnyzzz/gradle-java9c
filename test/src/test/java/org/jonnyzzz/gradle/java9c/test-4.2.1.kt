package org.jonnyzzz.gradle.java9c

import org.junit.Test


open class IntegrationTest_4_2_1 : IntegrationTest_4_4_1() {
  override val gradleVersion = "4.2.1"

  @Test
  override fun test_java9_module_info() {
    super.test_java9_module_info()
  }
}
