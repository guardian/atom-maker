package com.gu.atom.publish

import com.amazonaws.services.kinesis.AmazonKinesis
import com.gu.contentatom.thrift.ContentAtomEvent
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest

import scala.concurrent.{ExecutionContext, Future}

class KinesisAtomReindexerV2(streamName: String,
                              kinesis: KinesisClient)
  extends AtomReindexer
    with ThriftSerializer[ContentAtomEvent] {

  def makePartitionKey(event: ContentAtomEvent): String = event.atom.atomType.name

  override def startReindexJob(atomEventsToReindex: Iterator[ContentAtomEvent], expectedSize: Int): AtomReindexJob =  new AtomReindexJob(atomEventsToReindex, expectedSize) {
    def execute(implicit ec: ExecutionContext) = Future {
      atomEventsToReindex foreach { atomEvent =>
        val putRecordRequest = PutRecordRequest.builder().streamName(streamName)
                                .data(SdkBytes.fromByteBuffer(serializeEvent(atomEvent)))
                                .partitionKey(makePartitionKey(atomEvent)).build()

        kinesis.putRecord(putRecordRequest)
        _completedCount += 1
      }
      _isComplete = true
      _completedCount
    }
  }
}

class PreviewKinesisAtomReindexerV2(val streamName: String,
                                   val kinesis: KinesisClient)
  extends KinesisAtomReindexerV2(streamName, kinesis) with PreviewAtomReindexer

class PublishedKinesisAtomReindexerV2( val streamName: String,
                                     val kinesis: KinesisClient)
  extends KinesisAtomReindexerV2(streamName, kinesis) with PublishedAtomReindexer
