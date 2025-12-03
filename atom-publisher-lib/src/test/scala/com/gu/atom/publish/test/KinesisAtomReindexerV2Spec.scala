package com.gu.atom.publish.test

import software.amazon.awssdk.services.kinesis.KinesisClient
import com.gu.atom.TestData
import com.gu.atom.publish.{PreviewKinesisAtomReindexerV2, PublishedKinesisAtomReindexer, PublishedKinesisAtomReindexerV2}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class KinesisAtomReindexerV2Spec
    extends AnyFunSpecLike
    with Matchers
    with ScalaFutures
    with MockitoSugar {

  describe("Preview Kinesis Atom Reindexer") {

    it("should call putRecords() for each atom") {
      val kinesis = mock[KinesisClient]
      val reindexer = new PreviewKinesisAtomReindexerV2("testStream", kinesis)
      val expectedCount = TestData.testAtoms.size
      val job = reindexer.startReindexJob(TestData.testAtomEvents.iterator, expectedCount)
      job.expectedSize should equal(expectedCount)
      whenReady(job.execute, timeout(13.seconds)) { completedCount =>
        completedCount should equal(expectedCount)
        job.completedCount should equal(expectedCount)
        job.isComplete should equal(true)
        verify(kinesis, times(expectedCount)).putRecord(
          argThat { req: PutRecordRequest =>
            req.streamName() == "testStream" &&
              req.data().asByteArray().nonEmpty &&
              req.partitionKey() == "Media"
          }
        )
      }
    }
  }

  describe("Live Kinesis Atom Reindexer") {

    it("should call putRecords() for each atom") {
      val kinesis = mock[KinesisClient]
      val reindexer = new PublishedKinesisAtomReindexerV2("testStream", kinesis)
      val expectedCount = TestData.testAtoms.size
      val job = reindexer.startReindexJob(TestData.testAtomEvents.iterator, expectedCount)
      job.expectedSize should equal(expectedCount)
      whenReady(job.execute, timeout(13.seconds)) { completedCount =>
        completedCount should equal(expectedCount)
        job.completedCount should equal(expectedCount)
        job.isComplete should equal(true)
        verify(kinesis, times(expectedCount)).putRecord(argThat { req: PutRecordRequest =>
          req.streamName() == "testStream" &&
            req.data().asByteArray().nonEmpty &&
            req.partitionKey() == "Media"
        })
      }
    }
  }
}
