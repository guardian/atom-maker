package com.gu.atom.publish.test

import com.gu.atom.TestData.testAtom
import com.gu.atom.data.{AtomDataStore, ReadError}
import com.gu.atom.reindex.{PreviewKinesisAtomReindexerV2, ReindexDataStore, ReindexJob}
import org.mockito.ArgumentMatchers.{any, anyLong, argThat}
import org.mockito.Mockito.{never, timeout, verify, when}
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.{PutRecordRequest, PutRecordResponse}

class KinesisAtomReindexerV2Spec
    extends AnyFunSpecLike
    with Matchers
    with MockitoSugar {

  private def inProgressJob(documentsExpected: Long): ReindexJob =
    ReindexJob(
      status = ReindexJob.inProgress,
      startedAt = "now",
      documentsExpected = documentsExpected,
      documentsIndexed = 0
    )

  describe("Preview Kinesis Atom Reindexer") {

    it("returns Failure from startReindex when itemCount fails") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      when(atomDataStore.itemCount).thenReturn(Left(ReadError))

      reindexer.startReindex().isFailure shouldBe true
      verify(reindexDataStore, never).create(anyLong())
      verify(kinesis, never).putRecord(any[PutRecordRequest])
    }

    it("returns Failure from startReindex when create cannot create a new job") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      when(atomDataStore.itemCount).thenReturn(Right(5L))
      when(reindexDataStore.create(5L)).thenReturn(None)

      reindexer.startReindex().isFailure shouldBe true
      verify(atomDataStore, never).scanPage(any())
      verify(kinesis, never).putRecord(any[PutRecordRequest])
    }

    it("reindexes paged atoms and records progress until complete") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      val job = inProgressJob(3)
      val pageOneAtoms = List(testAtom("1"), testAtom("2"))
      val pageTwoAtoms = List(testAtom("3"))
      val continuationKey = new java.util.HashMap[String, software.amazon.awssdk.services.dynamodb.model.AttributeValue]()

      when(atomDataStore.itemCount).thenReturn(Right(3L))
      when(reindexDataStore.create(3L)).thenReturn(Some(job))
      when(reindexDataStore.getInProgress()).thenReturn(Some(job), Some(job))
      when(atomDataStore.scanPage(any()))
        .thenReturn(
          Right((pageOneAtoms, Some(continuationKey))),
          Right((pageTwoAtoms, None))
        )
      when(kinesis.putRecord(any[PutRecordRequest]))
        .thenReturn(PutRecordResponse.builder().build())

      reindexer.startReindex().get shouldBe job

      verify(reindexDataStore, timeout(2000)).recordProgress(2L)
      verify(reindexDataStore, timeout(2000)).recordProgress(3L)
      verify(kinesis, timeout(2000).times(3)).putRecord(any[PutRecordRequest])
      verify(reindexDataStore, timeout(2000)).markComplete(argThat { completedJob: ReindexJob =>
        completedJob.documentsIndexed == 3L
      })
    }

    it("marks job failed and does not record progress when scan fails") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      val job = inProgressJob(1)
      when(atomDataStore.itemCount).thenReturn(Right(1L))
      when(reindexDataStore.create(1L)).thenReturn(Some(job))
      when(reindexDataStore.getInProgress()).thenReturn(Some(job))
      when(atomDataStore.scanPage(any())).thenReturn(Left(ReadError))

      reindexer.startReindex().get shouldBe job

      verify(reindexDataStore, timeout(2000)).markFailed(job)
      verify(reindexDataStore, never).recordProgress(anyLong())
      verify(kinesis, never).putRecord(any[PutRecordRequest])
    }

    it("delegates getReindexStatus to reindexDataStore") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      val job = inProgressJob(10)
      when(reindexDataStore.get()).thenReturn(Some(job))

      reindexer.getReindexStatus() shouldBe Some(job)
    }

    it("cancels an in-progress job and returns it with cancelled status") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      val job = inProgressJob(8)
      when(reindexDataStore.getInProgress()).thenReturn(Some(job))
      when(reindexDataStore.markCancelled(job)).thenReturn(job.copy(status = ReindexJob.cancelled))

      reindexer.cancelReindex() shouldBe Some(job.copy(status = ReindexJob.cancelled))
      verify(reindexDataStore).markCancelled(job)
    }

    it("returns None from cancelReindex when there is no in-progress job") {
      val kinesis = mock[KinesisClient]
      val atomDataStore = mock[AtomDataStore]
      val reindexDataStore = mock[ReindexDataStore]
      val reindexer = new PreviewKinesisAtomReindexerV2("preview", kinesis, atomDataStore, reindexDataStore)

      when(reindexDataStore.getInProgress()).thenReturn(None)

      reindexer.cancelReindex() shouldBe None
      verify(reindexDataStore, never).markCancelled(any[ReindexJob])
    }
  }
}
