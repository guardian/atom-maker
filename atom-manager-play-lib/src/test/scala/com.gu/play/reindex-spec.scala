package com.gu.atom.play.test

import com.gu.atom.play.ReindexController
import com.gu.atom.publish._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar.mock

class ReindexSpec extends AtomSuite {

  override implicit lazy val app = play.api.test.FakeApplication(
    additionalConfiguration = Map("reindexApiKey" -> "xyzzy")
  )

  def mockReindexer[T <: AtomReindexer : Manifest]: T = {
    val r = mock[T]
    when(r.startReindexJob(any(), any())).thenReturn(AtomReindexJob.empty)
    r
  }

  val publishedAtomReindexer = mockReindexer[PublishedAtomReindexer]

  val previewAtomReindexer = mockReindexer[PreviewAtomReindexer]

  lazy val reindexCtrl = new ReindexController(
    initialPreviewDataStore,
    initialPublishedDataStore,
    previewAtomReindexer,
    publishedAtomReindexer,
    app.configuration,
    app.actorSystem)

  val reindexApiKey = "xyzzy"

  // override def customOverrides = {

  //   super.customOverrides :+ mbind[PublishedAtomReindexer] { (r: AtomReindexer) =>
  //     when(r.startReindexJob(any(), any())).thenReturn(AtomReindexJob.empty)
  //   } :+ mbind[PreviewAtomReindexer] { (r: AtomReindexer) =>
  //     when(r.startReindexJob(any(), any())).thenReturn(AtomReindexJob.empty)
  //   }

  // }
  // override def customConfig = super.customConfig + ("reindexApiKey" -> reindexApiKey)

  "preview reindex api" should {
    "deny access without api key or with incorrect key" in {
      (status(reindexCtrl.newPreviewReindexJob().apply(FakeRequest()))
         mustEqual UNAUTHORIZED)

      (status(reindexCtrl.newPreviewReindexJob().apply(FakeRequest("GET", s"/?api=jafklsj")))
         mustEqual UNAUTHORIZED)
    }
  }

  "publish reindex api" should {
    "deny access without api key or with incorrect key" in {
      (status(reindexCtrl.newPublishedReindexJob().apply(FakeRequest()))
        mustEqual UNAUTHORIZED)

      (status(reindexCtrl.newPublishedReindexJob().apply(FakeRequest("GET", s"/?api=jafklsj")))
        mustEqual UNAUTHORIZED)
    }
  }

}
