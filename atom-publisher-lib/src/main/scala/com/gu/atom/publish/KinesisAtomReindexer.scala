package com.gu.atom.publish

import com.amazonaws.services.kinesis.AmazonKinesis
import com.gu.contentatom.thrift.ContentAtomEvent

import scala.concurrent.{ExecutionContext, Future}

class KinesisAtomReindexer(
  streamName: String,
  kinesis: AmazonKinesis)
    extends AtomReindexer
    with ThriftSerializer[ContentAtomEvent] {

  def makePartitionKey(event: ContentAtomEvent): String = event.atom.atomType.name

  def startReindexJob(atomEventsToReindex: Iterator[ContentAtomEvent], expectedSize: Int) =
    new AtomReindexJob(atomEventsToReindex, expectedSize) {
      def execute(implicit ec: ExecutionContext) = Future {
        atomEventsToReindex foreach { atomEvent =>
          kinesis.putRecord(streamName, serializeEvent(atomEvent), makePartitionKey(atomEvent))
          _completedCount += 1
        }
        _isComplete = true
        _completedCount
      }
    }
}

class PreviewKinesisAtomReindexer( val streamName: String,
                                   val kinesis: AmazonKinesis)
  extends KinesisAtomReindexer(streamName, kinesis) with PreviewAtomReindexer

class PublishedKinesisAtomReindexer( val streamName: String,
                                     val kinesis: AmazonKinesis)
  extends KinesisAtomReindexer(streamName, kinesis) with PublishedAtomReindexer

 
