package ru.clustering.load_balancing

object LoadBalancingApp extends App {
//TODO - разобраться с конфигурацией (не запускается)

  //initiate three nodes from backend
  Backend.initiate(2551)

  Backend.initiate(2552)

  Backend.initiate(2561)

  //initiate frontend node
  Frontend.initiate()

}