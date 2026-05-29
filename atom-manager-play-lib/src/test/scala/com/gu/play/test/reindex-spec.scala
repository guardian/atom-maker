package com.gu.play.test

import com.gu.atom.play.ReindexController
import com.gu.atom.reindex._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.util.Success

class ReindexSpec extends AtomSuite {

  def reindexCtrl(implicit c: AtomTestConf) = c.app.injector.instanceOf[ReindexController]

  val reindexApiKey = "xyzzy"

  def newReindexJob = ReindexJob(
    status = ReindexJob.inProgress, startedAt = "123", documentsExpected = 500, documentsIndexed = 0
  )

  override def customOverrides = {
    super.customOverrides :+ mbind[PublishedAtomReindexer] { (r: AtomReindexer) =>
      when(r.startReindex()).thenReturn(Success(newReindexJob))
    } :+ mbind[PreviewAtomReindexer] { (r: AtomReindexer) =>
      when(r.startReindex()).thenReturn(Success(newReindexJob))
    }
  }

  override def customConfig = super.customConfig + ("reindexApiKey" -> reindexApiKey)

  "preview reindex api" should {
    "deny access without api key or with incorrect key" in AtomTestConf() { implicit conf =>
      (status(reindexCtrl.newPreviewReindexJob().apply(FakeRequest()))
         mustEqual UNAUTHORIZED)

      (status(reindexCtrl.newPreviewReindexJob().apply(FakeRequest("GET", s"/?api=jafklsj")))
         mustEqual UNAUTHORIZED)
    }
  }

  "publish reindex api" should {
    "deny access without api key or with incorrect key" in AtomTestConf() { implicit conf =>
      (status(reindexCtrl.newPublishedReindexJob().apply(FakeRequest()))
        mustEqual UNAUTHORIZED)

      (status(reindexCtrl.newPublishedReindexJob().apply(FakeRequest("GET", s"/?api=jafklsj")))
        mustEqual UNAUTHORIZED)
    }
  }

}
