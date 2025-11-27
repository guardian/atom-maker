package com.gu.atom.publish

import com.gu.contentatom.thrift.ContentAtomEvent
import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.awssdk.services.kinesis.KinesisClient

import scala.util.Try

class KinesisAtomPublisherV2(val streamName: String, val kinesis: KinesisClient)  extends AtomPublisher
  with ThriftSerializer[ContentAtomEvent]
  with LazyLogging {
  logger.info(s"KinesisAtomPublisher started with streamName $streamName")
  def makePartitionKey(event: ContentAtomEvent): String = event.atom.atomType.name

  override def publishAtomEvent(event: ContentAtomEvent): Try[Unit] = Try {
    val putRecordRequest = PutRecordRequest.builder().streamName(streamName)
                            .data(SdkBytes.fromByteBuffer(serializeEvent(event)))
                            .partitionKey(makePartitionKey(event)).build()
    kinesis.putRecord(putRecordRequest)
    ()
  }
}

class PreviewKinesisAtomPublisherV2(override val streamName: String,
                                  override val kinesis: KinesisClient)
  extends KinesisAtomPublisherV2(streamName, kinesis) with PreviewAtomPublisher

class LiveKinesisAtomPublisherV2(override val streamName: String,
                               override val kinesis: KinesisClient)
  extends KinesisAtomPublisherV2(streamName, kinesis) with LiveAtomPublisher
