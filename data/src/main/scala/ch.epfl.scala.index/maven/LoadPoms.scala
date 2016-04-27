package ch.epfl.scala.index
package maven

import me.tongfei.progressbar._

import java.io.File
import java.nio.file._
import scala.util.Try
import java.util.Properties

case class MissingParentPom(dep: maven.Dependency) extends Exception

object Poms {
  import org.apache.maven.model._
  import resolution._
  import io._
  import building._ 

  val parentPomsBase = bintray.bintrayIndexBase.resolve("poms_parent")

  private val builder = (new DefaultModelBuilderFactory).newInstance
  private val processor = new DefaultModelProcessor
  processor.setModelReader(new DefaultModelReader)

  def path(dep: maven.Dependency) = {
    import dep._
    List(
      groupId.replaceAllLiterally(".", "/"),
      artifactId,
      version,
      artifactId + "-" + version + ".pom"
    ).mkString(File.separator)
  }

  private val resolver = new ModelResolver {
    def addRepository(repo: Repository, replace: Boolean): Unit = ()
    def addRepository(repo: Repository): Unit = ()
    def newCopy(): resolution.ModelResolver = throw new Exception("copy")   
    def resolveModel(parent: Parent): ModelSource2 = {
      resolveModel(parent.getGroupId, parent.getArtifactId, parent.getVersion)
    }
    def resolveModel(groupId: String, artifactId: String, version: String): ModelSource2 = {
      val dep = maven.Dependency(groupId, artifactId, version)
      val target = parentPomsBase.resolve(path(dep))

      if(Files.exists(target)) {
        new FileModelSource(target.toFile)
      } else throw new MissingParentPom(dep)
    }
  }

  private val jdk = new Properties
  jdk.setProperty("java.version", "1.8") // << ???
  // jdk.setProperty("scala.version", "2.11.7")
  // jdk.setProperty("scala.binary.version", "2.11")

  private def resolve(pom: Path) = {
    val request = new DefaultModelBuildingRequest
    request
      .setModelResolver(resolver)
      .setSystemProperties(jdk)
      .setPomFile(pom.toFile)

    builder.build(request).getEffectiveModel
  }

  def load(): List[Try[Model]]  = {
    import scala.collection.JavaConverters._

    val s = Files.newDirectoryStream(bintray.bintrayPomBase)
    val poms = s.asScala.toList

    val progress = new ProgressBar("Reading POMs", poms.size)
    progress.start()

    val parentPomsToDownload = 
      poms.map{p =>
        progress.step()
        Try(resolve(p))
      }
    progress.stop()
    s.close()

    parentPomsToDownload
  }
  lazy val get = load()
}