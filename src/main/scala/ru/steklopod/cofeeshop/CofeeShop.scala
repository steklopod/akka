package ru.steklopod.cofeeshop
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.steklopod.cofeeshop.Order._
import scala.concurrent.Future
import scala.concurrent.duration._

object CofeeShop extends App {
  val system           = ActorSystem("CoffeeShop")
  implicit val timeout = Timeout(2 second)
  implicit val ec      = system.dispatcher

  val barista  = system.actorOf(Props[Barista], "Barista")
  val customer = system.actorOf(Props(classOf[Customer], barista), "Customer")

  val capuchinoPrice: Future[Any] = barista ? CappuccinoRequest
  capuchinoPrice.map { case Bill(cents) => println(s"Будут платить $cents р. за капучино") }

  customer ! CaffeineWithdrawalWarning
  barista  ! ClosingTime

//  println("Я заказал капучино и эспрессо")
}

object Order {
  sealed trait CoffeeRequest
  case object CappuccinoRequest extends CoffeeRequest
  case object EspressoRequest   extends CoffeeRequest
  case class Bill(cents: Int)
}

case object ClosingTime
case object CaffeineWithdrawalWarning

class Barista extends Actor {
  var cappuccinoCount = 0
  var espressoCount   = 0

  def receive = {
    case CappuccinoRequest =>
      println("Я должен приготовить капучино!")
      sender ! Bill(250)
      cappuccinoCount += 1
    case EspressoRequest =>
      println("Давайте приготовим эспрессо.")
      sender ! Bill(200)
      espressoCount += 1
    case ClosingTime => context.system.terminate()
  }
}

class Customer(caffeineSource: ActorRef) extends Actor {
  def receive = {
    case CaffeineWithdrawalWarning => caffeineSource ! EspressoRequest
    case Bill(cents)               => println(s"Я должен заплатить $cents р.!")
  }
}
