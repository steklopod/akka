package ru.steklopod.cofeeshop
import akka.actor.{Actor, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import ru.steklopod.cofeeshop.Barista.EspressoCup.Filled
import ru.steklopod.cofeeshop.Barista.{CappuccinoRequest, ClosingTime, EspressoCup, EspressoRequest}
import ru.steklopod.cofeeshop.Register.{Bill, Espresso, Transaction}

import scala.concurrent.duration._

class Barista extends Actor {
  import context.dispatcher

  var cappuccinoCount = 0
  var espressoCount   = 0

  implicit val timeout = Timeout(4.seconds)

  val register = context.actorOf(Props[Register], "Register")

  def receive = {
    case CappuccinoRequest =>
      println("Я должен приготовить капучино!")
      sender ! Bill(250)
      cappuccinoCount += 1

    case EspressoRequest =>
      println("Я должен приготовить эспрессо.")
      val receipt = register ? Transaction(Espresso)
      receipt.map((EspressoCup(Filled), _)).pipeTo(sender)
      espressoCount += 1

    case ClosingTime =>
      context.stop(self)
      sender ! Bill(200)
    case ClosingTime => context.system.terminate()
  }
}


object Barista {
  case object CappuccinoRequest
  case object EspressoRequest
  case object ClosingTime
  case class EspressoCup(state: EspressoCup.State)

  object EspressoCup {
    sealed trait State
    case object Clean  extends State
    case object Filled extends State
    case object Dirty  extends State
  }

  case class Receipt(amount: Int)
}
