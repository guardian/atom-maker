package com.gu.atom.play.test

import java.util.Date

import com.gu.atom.TestData._
import com.gu.atom.play._
import com.gu.contentatom.thrift._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.Inside
import play.api.mvc.Controller
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.scalatestplus.play.OneAppPerTest

class AtomAPIActionsSpec extends AtomSuite with OneAppPerTest with Inside {

  override def initialLivePublisher = defaultMockPublisher

  override def initialPublishedDataStore = {
    val m = publishedDataStoreMockWithTestData
    when(m.updateAtom(any())).thenReturn(Right(testAtom))
    m
  }

  def makeApiActions = new Controller with AtomAPIActions {
    val livePublisher = initialLivePublisher
    val previewPublisher = initialPreviewPublisher
    val previewDataStore = initialPreviewDataStore
    val publishedDataStore = initialPublishedDataStore
  }

  "api publish action" should {
    "succeed with NO_CONTENT" in {
      val apiActions = makeApiActions
      val result = call(apiActions.publishAtom("1"), FakeRequest())
      status(result) mustEqual NO_CONTENT
    }

    "update publish time and version for atom" in {
      val apiActions = makeApiActions
      val startTime = new Date().getTime
      val atomCaptor = ArgumentCaptor.forClass(classOf[Atom])
      val result = call(apiActions.publishAtom("1"), FakeRequest())
      status(result) mustEqual NO_CONTENT
      verify(apiActions.publishedDataStore).updateAtom(atomCaptor.capture())
      
      inside(atomCaptor.getValue) {
        case Atom("1", _, _, _, _, changeDetails, _, _) => {
          changeDetails.published.value.date must be >= startTime
          changeDetails.revision mustEqual 2
        }
      }
    }

  }

}
