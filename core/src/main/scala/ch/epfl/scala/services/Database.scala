package ch.epfl.scala.services

import scala.concurrent.Future

import ch.epfl.scala.index.newModel.NewProject

trait Database {
  def insetProject(p: NewProject): Future[NewProject]
}
