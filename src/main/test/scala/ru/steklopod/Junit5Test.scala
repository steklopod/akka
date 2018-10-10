package ru.steklopod

import org.junit.jupiter.api.{DisplayName, Test}
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, Matchers}
import org.scalatest.junit.JUnitSuite

@RunWith(classOf[JUnitPlatform])
class Junit5Test  extends JUnitSuite with Matchers with BeforeAndAfterAll {
  //http://www.scalatest.org/user_guide/using_junit_runner

  override def beforeAll: Unit = { println(">>> beforeAll <<< ") }

  @Test
  @DisplayName("Example with JUnitSuite")
  @throws(classOf[RuntimeException])
  def throwsExceptionWhenCalled() {
    println("Запуск теста...")
    assertThrows[RuntimeException] { throw new RuntimeException }
  }

}