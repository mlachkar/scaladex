package ch.epfl.scala.services.storage.sql

import cats.effect.{IO, Resource}
import ch.epfl.scala.index.model.Project
import doobie.implicits._
import ch.epfl.scala.index.newModel.NewProject
import ch.epfl.scala.services.Database
import ch.epfl.scala.services.storage.sql.tables.ProjectTable
import ch.epfl.scala.utils.DoobieUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ScaladexRepo(conf: DbConf) extends Database {
  private[sql] val (xa, flyway) = DoobieUtils.create(conf)
  def createTables(): IO[Unit] = IO(flyway.migrate())
  def dropTables(): IO[Unit] = IO(flyway.clean())

  override def insetProject(project: NewProject): Future[NewProject] = {
    run(ProjectTable.insert, project)
  }

  private def run[A](i: A => doobie.Update0, v: A): Future[A] =
    i(v).run.transact(xa).unsafeToFuture.flatMap {
      case 1 => Future.successful(v)
      case code =>
        Future.failed(new Exception(s"Failed to insert $v (code: $code)"))
    }

}
