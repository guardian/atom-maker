package com.gu.atom.play

import java.util.Date
import javax.inject.{Inject, Singleton}
import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import com.gu.atom.data._
import com.gu.atom.play.ReindexActor._
import com.gu.atom.publish._
import com.gu.contentatom.thrift.{Atom, ContentAtomEvent, EventType}
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/*
 * In here we find the Actor that is responsible for initialising,
 * monitoring, and finishing the reindex job.
 *
 * The controller will send it messages to get a status update and so on.
 */

class ReindexActor(reindexer: AtomReindexer) extends Actor {
  implicit val ec: ExecutionContext = context.dispatcher

  /* this is the initial, idle state. In this state we will accept new jobs */
  def idleState(lastJob: Option[AtomReindexJob]): Receive = {
    case CreateJob(atoms, expectedSize) =>
      val job = reindexer.startReindexJob(atoms, expectedSize)
      context.become(inProgressState(job))
      job.execute.onComplete {
        case _ => context.become(idleState(Some(job)), true)
      }
      sender() ! RSuccess

    case GetStatus =>
      sender() ! lastJob.map(statusReply)
  }

  def inProgressState(job: AtomReindexJob): Receive = {
    case CreateJob(_, _) =>
      sender() ! RFailure("in progress")
    case GetStatus =>
      sender() ! Some(statusReply(job))
  }

  /* start off in idle state with no record of a previous job */
  def receive = idleState(None)

}

/* the messages that we will send and receive */
object ReindexActor {
  /* requests */
  case class CreateJob(atoms: Iterator[ContentAtomEvent], expectedSize: Int)
  case object GetStatus

  /* responses */
  case object RSuccess
  case class RFailure(reason: String)

  /* matches response expected by CAPI */
  object StatusType extends Enumeration {
    val inProgress = Value("in progress")
    val failed     = Value("failed")
    val completed  = Value("completed")
    val cancelled  = Value("cancelled")
  }

  case class JobStatus(
    status: StatusType.Value,
    documentsIndexed: Int,
    documentsExpected: Int
  )

  def statusReply(job: AtomReindexJob): JobStatus =
    JobStatus(
      if(job.isComplete) StatusType.completed else StatusType.inProgress,
      job.completedCount,
      job.expectedSize
    )
}

@Singleton
class ReindexController @Inject() (
                                   previewDataStore: PreviewDataStore,
                                   publishedDataStore: PublishedDataStore,
                                   previewReindexer: PreviewAtomReindexer,
                                   publishedReindexer: PublishedAtomReindexer,
                                   config: Configuration,
                                   val controllerComponents: ControllerComponents,
                                   system: ActorSystem) extends BaseController {

  def now() = new Date().getTime

  implicit val ec: ExecutionContext = system.dispatcher

  val previewReindexActor = system.actorOf(Props(classOf[ReindexActor], previewReindexer))
  val publishedReindexActor = system.actorOf(Props(classOf[ReindexActor], publishedReindexer))

  implicit val timeout: Timeout = Timeout(5.seconds)

  implicit val statusWrites: Writes[JobStatus] = Json.writes[JobStatus]

  object ApiKeyAction extends ActionBuilder[Request, AnyContent] {
    lazy val apiKey = config.get[String]("reindexApiKey")

    def invokeBlock[A](request: Request[A], block: (Request[A] => Future[Result])) = {
      if(request.getQueryString("api").contains(apiKey))
        block(request)
      else
        Future.successful(Unauthorized(""))
    }

    override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = controllerComponents.executionContext
  }

  def newPreviewReindexJob = getNewReindexJob(previewDataStore.listAtoms, previewReindexActor)
  def newPublishedReindexJob = getNewReindexJob(publishedDataStore.listAtoms, publishedReindexActor)

  def previewReindexJobStatus = getReindexJobStatus(previewReindexActor)
  def publishedReindexJobStatus = getReindexJobStatus(publishedReindexActor)

  private def getReindexJobStatus(actor: ActorRef) =  ApiKeyAction.async { implicit req =>
    (actor ? GetStatus) map {
      case None => NotFound("")
      case Some(job: JobStatus) => Ok(Json.toJson(job))
      case _ => InternalServerError("unknown-error")
    }
  }

  private def getNewReindexJob(getAtoms: previewDataStore.DataStoreResult[Seq[Atom]], actor: ActorRef) =
    ApiKeyAction.async { implicit req =>
      getAtoms.fold(

        { err =>
          Future.successful(InternalServerError(err.toString))
        },

        { atoms =>
          val events = atoms.map(atom => ContentAtomEvent(atom, EventType.Update, now())).toList
          (actor ? CreateJob(events.iterator, events.size)) map {
            case RSuccess      => Ok("")
            case RFailure(msg) => InternalServerError(s"could't create job: $msg")
            case _ => InternalServerError("unknown error")
          }
        }

      )
    }

}
