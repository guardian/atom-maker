package com.gu.atom.play

import com.gu.atom.reindex.{AtomReindexer, PreviewAtomReindexer, PublishedAtomReindexer}
import io.circe.syntax.EncoderOps
import org.apache.pekko.actor.ActorSystem
import play.api.Configuration
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ReindexController @Inject()(
  previewReindexer: PreviewAtomReindexer,
  publishedReindexer: PublishedAtomReindexer,
  config: Configuration,
  val controllerComponents: ControllerComponents,
  system: ActorSystem
) extends BaseController {

  implicit val ec: ExecutionContext = system.dispatcher

  private object ApiKeyAction extends ActionBuilder[Request, AnyContent] {
    private lazy val apiKey = config.get[String]("reindexApiKey")

    def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
      if(request.getQueryString("api").contains(apiKey))
        block(request)
      else
        Future.successful(Unauthorized(""))
    }

    override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = controllerComponents.executionContext
  }

  def newPreviewReindexJob: Action[AnyContent] = getNewReindexJob(previewReindexer)
  def newPublishedReindexJob: Action[AnyContent] = getNewReindexJob(publishedReindexer)

  // TODO add tests?
  def previewReindexJobStatus: Action[AnyContent] = getReindexJobStatus(previewReindexer)
  def publishedReindexJobStatus: Action[AnyContent] = getReindexJobStatus(publishedReindexer)

  // TODO add tests?
  def cancelPreviewReindexJob: Action[AnyContent] = cancelReindexJob(previewReindexer)
  def cancelPublishedReindexJob: Action[AnyContent] = cancelReindexJob(publishedReindexer)

  private def getReindexJobStatus(reindexer: AtomReindexer) =
    ApiKeyAction {
      reindexer.getReindexStatus() match {
        case None => NotFound("No active reindexes")
        case Some(reindexJob) => Ok(reindexJob.asJson.spaces2).as(JSON)
      }
    }

  private def getNewReindexJob(reindexer: AtomReindexer) =
    ApiKeyAction {
      reindexer.startReindex() match {
        case Failure(err) => InternalServerError(err.toString)
        case Success(reindexJob) => Ok(reindexJob.asJson.spaces2).as(JSON)
      }
    }

  private def cancelReindexJob(reindexer: AtomReindexer) =
    ApiKeyAction {
      reindexer.cancelReindex() match {
        case None => NotFound
        case Some(reindexJob) => Ok(reindexJob.asJson.spaces2).as(JSON)
      }
    }

}
