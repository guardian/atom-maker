package com.gu.atom.play

import java.util.Date

import com.gu.atom.data._
import com.gu.atom.publish._
import com.gu.atom.util.AtomImplicitsGeneral._
import com.gu.contentatom.thrift._
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc._

import scala.util.{Failure, Success}

trait AtomAPIActions[ATOM <: Atom] extends BaseController {

  val livePublisher: LiveAtomPublisher
  val previewPublisher: PreviewAtomPublisher
  val previewDataStore: PreviewDataStore[ATOM]
  val publishedDataStore: PublishedDataStore[ATOM]

  private def jsonError(msg: String): JsObject = JsObject(Seq("error" -> JsString(msg)))

  def publishAtom(atomId: String) = Action { implicit req =>

    val revisionNumber = publishedDataStore.getAtom(atomId) match {
      case Right(atom) => atom.contentChangeDetails.revision + 1
      case Left(_) => 1
    }

    previewDataStore.getAtom(atomId) match {
      case Right(atom) => {
        val updatedAtom = atom.copy(
          contentChangeDetails = atom.contentChangeDetails.copy(published = Some(ChangeRecord(new Date().getTime, None)))
        ).withRevision(revisionNumber)

        savePublishedAtom(updatedAtom)
      }
      case Left(IDNotFound) => NotFound(jsonError(s"No such atom $atomId"))
      case Left(error) => InternalServerError(s"Could not publish $error")
    }
  }

  private def savePublishedAtom(updatedAtom: Atom) = {
    val event = ContentAtomEvent(updatedAtom, EventType.Update, new Date().getTime)
    livePublisher.publishAtomEvent(event) match {
      case Success(_) =>
        publishedDataStore.updateAtom(updatedAtom) match {
          case Right(_) => NoContent
          case Left(err) => InternalServerError(
            jsonError(s"could not update after publish: ${err.toString}")
          )
        }
      case Failure(err) => InternalServerError(jsonError(s"could not publish: ${err.toString}"))
    }
  }
}
