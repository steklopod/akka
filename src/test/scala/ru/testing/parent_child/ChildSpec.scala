package ru.testing.parent_child

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

class ChildSpec extends TestKit(ActorSystem("test-system")) 
                  with ImplicitSender
                  with FlatSpecLike
                  with BeforeAndAfterAll 
                  with MustMatchers {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Child Actor" should "send pong message when receive ping message" in {
    val parent  = TestProbe()

    val child = system.actorOf(Props(new Child(parent.ref)))

    child ! "ping"

    parent.expectMsg("pong")
  }

}