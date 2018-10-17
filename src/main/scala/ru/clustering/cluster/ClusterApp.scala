package ru.clustering.cluster

import ru.clustering.commons._


object ClusterApp extends App {
  //initiate frontend node
  Frontend.initiate()

  //initiate three nodes from backend
  Backend.initiate(2552)

  Backend.initiate(2560)

  Backend.initiate(2561)

  Thread.sleep(10000)

  Frontend.getFrontend ! Add(2, 4)

}