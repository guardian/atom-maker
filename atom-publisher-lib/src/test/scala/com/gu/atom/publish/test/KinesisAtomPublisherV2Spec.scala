package com.gu.atom.publish.test

import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.awssdk.services.kinesis.KinesisClient
import com.gu.atom.TestData._
import com.gu.atom.publish.KinesisAtomPublisherV2
import org.mockito.ArgumentMatchers.{eq => argEq, _}
import org.mockito.Mockito._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.util.{Failure, Success}

class KinesisAtomPublisherV2Spec
    extends AnyFunSpec
    with Matchers
    with MockitoSugar {

  val streamName = "testStream"

  describe("Kinesis Atom Publisher") {
    it("should call putRecords()") {
      val kinesis = mock[KinesisClient]
      val testAtom = testAtomEvent()
      val publisher = new KinesisAtomPublisherV2(streamName, kinesis)

      val res = publisher.publishAtomEvent(testAtom)
      verify(kinesis).putRecord( argThat { req: PutRecordRequest =>
          req.streamName() == streamName &&
          req.partitionKey() == testAtom.atom.atomType.name &&
          req.data().asByteArray().nonEmpty
      })
      res should equal(Success(()))
    }

    it("should report exception") {
      val kinesis = mock[KinesisClient]
      val publisher = new KinesisAtomPublisherV2(streamName, kinesis)
      when(kinesis.putRecord(any[PutRecordRequest]))
        .thenThrow(classOf[RuntimeException])
      val res = publisher.publishAtomEvent(testAtomEvent())
      res should matchPattern { case Failure(e: RuntimeException) => }
    }
  }
}
