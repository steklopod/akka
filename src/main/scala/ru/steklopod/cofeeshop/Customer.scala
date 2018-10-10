package ru.steklopod.cofeeshop
import akka.actor.{Actor, ActorLogging, ActorRef}
import ru.steklopod.cofeeshop.Barista.EspressoCup.Filled
import ru.steklopod.cofeeshop.Barista.{EspressoCup, EspressoRequest, Receipt}
import ru.steklopod.cofeeshop.Customer.CaffeineWithdrawalWarning

class Customer(coffeeSource: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case CaffeineWithdrawalWarning              => coffeeSource ! EspressoRequest
    case (EspressoCup(Filled), Receipt(amount)) => log.info(s"Ёу! Кофеин для $self!")
  }
}

object Customer {
  case object CaffeineWithdrawalWarning
}
