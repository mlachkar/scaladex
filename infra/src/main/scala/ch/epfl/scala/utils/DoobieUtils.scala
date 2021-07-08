package ch.epfl.scala.utils

import cats.effect.{ContextShift, IO}
import ch.epfl.scala.services.storage.sql.DbConf
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie._
import doobie.util.fragment.Fragment
import doobie.implicits._
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object DoobieUtils {
  private implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  def create(conf: DbConf): (doobie.Transactor[IO], Flyway) = {
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    val xa: doobie.Transactor[IO] = conf match {
      case c: DbConf.H2 =>
        Transactor.fromDriverManager[IO](c.driver, c.url, "", "")
      case c: DbConf.PostgreSQL =>
        Transactor.fromDriverManager[IO](c.driver, c.url, c.user, c.pass.decode)
    }

    val config = new HikariConfig()
    conf match {
      case c: DbConf.H2 =>
        config.setDriverClassName(c.driver)
        config.setJdbcUrl(c.url)
      case c: DbConf.PostgreSQL =>
        config.setDriverClassName(c.driver)
        config.setJdbcUrl(c.url)
        config.setUsername(c.user)
        config.setPassword(c.pass.decode)
    }
    val flyway = Flyway
      .configure()
      .dataSource(new HikariDataSource(config))
      .locations("classpath:migrations")
      .load()

    (xa, flyway)
  }

  object Fragments {
    val empty = fr0""
    val space = fr0" "

    def buildInsert(
        table: Fragment,
        fields: Fragment,
        values: Fragment
    ): Fragment =
      fr"INSERT INTO" ++ table ++ fr0" (" ++ fields ++ fr0") VALUES (" ++ values ++ fr0")"

    def buildSelect(table: Fragment, fields: Fragment): Fragment =
      fr"SELECT" ++ fields ++ fr" FROM" ++ table

    def buildSelect(
        table: Fragment,
        fields: Fragment,
        where: Fragment
    ): Fragment =
      buildSelect(table, fields) ++ space ++ where

  }
}
