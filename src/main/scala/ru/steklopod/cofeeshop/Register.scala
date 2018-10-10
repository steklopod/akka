package ru.steklopod.cofeeshop
import akka.actor.Actor
import ru.steklopod.cofeeshop.Barista.Receipt
import ru.steklopod.cofeeshop.Register.{Article, Cappuccino, Espresso, Transaction}

//кассовый аппарат
class Register extends Actor {
  var revenue = 0
  val prices  = Map[Article, Int](Espresso -> 150, Cappuccino -> 250)

  def receive = {
    case Transaction(article) =>
      val price: Int = prices(article)
      sender ! createReceipt(price)
      revenue += price
  }

  def createReceipt(price: Int): Receipt = Receipt(price)
}

object Register {
  sealed trait Article

  case object Espresso   extends Article
  case object Cappuccino extends Article
  case class Bill(cents: Int)

  case class Transaction(article: Article)
}
