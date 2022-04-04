package com.gu.atom.publish

import com.amazonaws.services.kinesis.AmazonKinesis
import com.gu.contentatom.thrift.ContentAtomEvent
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

class KinesisAtomPublisher (val streamName: String, val kinesis: AmazonKinesis)
    extends AtomPublisher
    with ThriftSerializer[ContentAtomEvent]
    with LazyLogging
{

  logger.info(s"KinesisAtomPublisher started with streamName $streamName")

  def makePartitionKey(event: ContentAtomEvent): String = event.atom.atomType.name

  def publishAtomEvent(event: ContentAtomEvent): Try[Unit] = Try {
      val data = serializeEvent(event)
      kinesis.putRecord(streamName, data, makePartitionKey(event))
    }
}

class PreviewKinesisAtomPublisher(override val streamName: String,
                                  override val kinesis: AmazonKinesis)
  extends KinesisAtomPublisher(streamName, kinesis) with PreviewAtomPublisher

class LiveKinesisAtomPublisher(override val streamName: String,
                                  override val kinesis: AmazonKinesis)
  extends KinesisAtomPublisher(streamName, kinesis) with LiveAtomPublisher
