package ru.steklopod.cofeeshop
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.steklopod.cofeeshop.Barista._
import ru.steklopod.cofeeshop.Customer.CaffeineWithdrawalWarning
import ru.steklopod.cofeeshop.Register.Bill

import scala.concurrent.Future
import scala.concurrent.duration._

object CofeeShop extends App {
  val system           = ActorSystem("CoffeeShop")
  implicit val timeout = Timeout(2 second)
  implicit val ec      = system.dispatcher

  val barista        = system.actorOf(Props[Barista], "Barista")
  val customerJohnny = system.actorOf(Props(classOf[Customer], barista), "Johnny")
  val customerAlina  = system.actorOf(Props(classOf[Customer], barista), "Alina")

  customerJohnny ! CaffeineWithdrawalWarning
  customerAlina ! CaffeineWithdrawalWarning

  val capuchinoPrice: Future[Any] = barista ? CappuccinoRequest
  capuchinoPrice.map {
    case Bill(cents) => println(s"Будут платить $cents р. за капучино")
  }
}
