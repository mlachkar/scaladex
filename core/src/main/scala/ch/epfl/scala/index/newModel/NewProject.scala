package ch.epfl.scala.index.newModel

import ch.epfl.scala.index.model.Project
import ch.epfl.scala.index.newModel.utils.DataClass
import ch.epfl.scala.index.newModel.utils.UuidIdBuilder

case class NewProject(
    id: NewProject.Id,
    organization: String,
    repository: String
)

object NewProject {
  class Id private (value: String) extends DataClass(value)

  object Id
      extends UuidIdBuilder[NewProject.Id]("User.Id", new NewProject.Id(_))

  def from(p: Project): NewProject =
    NewProject(
      id = Id.generate(),
      organization = p.organization,
      repository = p.repository
    )

}
