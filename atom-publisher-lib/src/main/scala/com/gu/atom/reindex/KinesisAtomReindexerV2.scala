package com.gu.atom.reindex

import com.gu.atom.data.AtomDataStore
import com.gu.atom.publish.ThriftSerializer
import com.gu.contentatom.thrift.{Atom, ContentAtomEvent, EventType}
import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

import java.time.Instant
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class KinesisAtomReindexerV2(
  streamName: String,
  kinesis: KinesisClient,
  atomDataStore: AtomDataStore,
  override val reindexDataStore: ReindexDataStore,
) extends AtomReindexer with ThriftSerializer[ContentAtomEvent] with LazyLogging {

  def makePartitionKey(event: ContentAtomEvent): String = event.atom.atomType.name

  private def sendForReindex(atom: Atom) = {
    val atomEvent = ContentAtomEvent(atom, EventType.Update, Instant.now().toEpochMilli)
    val putRecordRequest = PutRecordRequest.builder().streamName(streamName)
      .data(SdkBytes.fromByteBuffer(serializeEvent(atomEvent)))
      .partitionKey(makePartitionKey(atomEvent)).build()
    kinesis.putRecord(putRecordRequest)
  }

  override def startReindex(): Try[ReindexJob] = {
    atomDataStore.itemCount match {
      case Left(reason) => Failure(reason)
      case Right(approximateItemCount) =>
        logger.info(s"Reindexing approximately $approximateItemCount items")

        val maybeNewReindexJob = reindexDataStore.create(approximateItemCount)
        maybeNewReindexJob
          .map { reindexJob =>
            logger.info("Created new reindex job: " + maybeNewReindexJob)
            run(reindexJob)(
              scala.concurrent.ExecutionContext.Implicits.global
            )
            Success(reindexJob)
          }
          .getOrElse {
            logger.warn("Could not create new reindex job; is there an active job already running?")
            Failure(new Exception(
              "Could not create new reindex job; is there an active job already running?"
            ))
          }
    }
  }

  override def getReindexStatus(): Option[ReindexJob] = {
    reindexDataStore.get()
  }

  override def cancelReindex(): Option[ReindexJob] = {
    logger.info("Attempting to cancel ongoing reindex...")
    reindexDataStore.getInProgress() match {
      case Some(reindexJob) =>
        reindexDataStore.markCancelled(reindexJob)
        logger.info("Reindex marked as cancelled")
        Some(reindexJob.copy(status = ReindexJob.cancelled))
      case None =>
        logger.warn("No ongoing reindex found, so nothing to cancel")
        None
    }
  }

  private def run(
    reindexJob: ReindexJob
  )(implicit ec: ExecutionContext) = {
    Future {
      val totalIndexedDocs = scan(reindexJob, lastEvaluatedKey = None, documentsIndexed = 0)
      reindexDataStore.markComplete(
        reindexJob.copy(documentsIndexed = totalIndexedDocs)
      )
    }
  }

  @tailrec
  private def scan(
    reindexJob: ReindexJob,
    lastEvaluatedKey: Option[atomDataStore.ContinuationKey],
    documentsIndexed: Long
  ): Long = {
    if (reindexDataStore.getInProgress().isEmpty) {
      logger.info("Stopping reindex as there is no in progress job anymore; probably cancelled")
      documentsIndexed
    } else {
      logger.info("Continuing reindex loop")
      val scanPage = atomDataStore.scanPage(lastEvaluatedKey)
      scanPage match {
        case Left(e) =>
          // TODO e
          reindexDataStore.markFailed(reindexJob)
          documentsIndexed
        case Right((atoms, continuationKey)) =>
          for (atom <- atoms) sendForReindex(atom)
          val newTotal = documentsIndexed + atoms.size
          logger.info(s"${atoms.size} atoms reindexed, bringing us to a total of $newTotal")
          reindexDataStore.recordProgress(newTotal)

          if (continuationKey.isEmpty) {
            logger.info("No more atoms to reindex, exiting loop")
            newTotal
          } else {
            scan(reindexJob, continuationKey, newTotal)
          }
      }
    }
  }
}

class PreviewKinesisAtomReindexerV2(
  streamName: String,
  kinesis: KinesisClient,
  atomDataStore: AtomDataStore,
  reindexDataStore: ReindexDataStore
) extends KinesisAtomReindexerV2(
  streamName, kinesis, atomDataStore, reindexDataStore
) with PreviewAtomReindexer

class PublishedKinesisAtomReindexerV2(
  streamName: String,
  kinesis: KinesisClient,
  atomDataStore: AtomDataStore,
  reindexDataStore: ReindexDataStore
) extends KinesisAtomReindexerV2(
  streamName, kinesis, atomDataStore, reindexDataStore
) with PublishedAtomReindexer
