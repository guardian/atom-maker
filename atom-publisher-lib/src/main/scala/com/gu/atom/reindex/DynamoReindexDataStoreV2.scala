package com.gu.atom.reindex

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import scala.jdk.CollectionConverters._

class DynamoReindexDataStoreV2(
  dynamoDbClient: DynamoDbClient,
  tableName: String,
) extends ReindexDataStore with LazyLogging {
  private val timestampFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  override def create(documentsExpected: Long): Option[ReindexJob] = {
    val newReindexJob = ReindexJob(
      status = ReindexJob.inProgress,
      startedAt = ZonedDateTime
        .now(ZoneId.of("Europe/London"))
        .format(timestampFormatter),
      documentsExpected = documentsExpected,
      documentsIndexed = 0
    )

    val putItemRequest = PutItemRequest
      .builder()
      .tableName(tableName)
      .item(itemFor(newReindexJob).asJava)
      .conditionExpression(s"attribute_not_exists(${ReindexJob.statusField})")
      .build()

    try {
      dynamoDbClient.putItem(putItemRequest)

      // Conditional insert past indiciating there was no active job registered
      // Clear down the previous completed job if there was one
      ReindexJob.completedJobStatuses.foreach { completionStatus =>
        val deleteItemRequest = DeleteItemRequest
          .builder()
          .tableName(tableName)
          .key(keyFor(completionStatus).asJava)
          .build()
        dynamoDbClient.deleteItem(deleteItemRequest)
      }
      Some(newReindexJob)

    } catch {
      case _: ConditionalCheckFailedException =>
        logger.warn(
          "ConditionalCheckFailedException; in progress reindex job detected"
        )
        None
      case e: Exception =>
        logger.error("Failed to insert new reindex job", e)
        None
    }
  }

  override def get(): Option[ReindexJob] = {
    val scanRequest = ScanRequest
      .builder()
      .tableName(tableName)
      .build()

    val response = dynamoDbClient.scan(scanRequest)

    response.items().asScala.map { item =>
      ReindexJob(
        status = item.get(ReindexJob.statusField).s(),
        startedAt = item.get(ReindexJob.startedAtField).s(),
        documentsExpected = item.get(ReindexJob.documentsExpectedField).n().toLong,
        documentsIndexed = item.get(ReindexJob.documentsIndexedField).n().toLong
      )
    }.maxByOption(_.startedAt)
  }

  override def getInProgress(): Option[ReindexJob] = {
    val getItemRequest = GetItemRequest
      .builder()
      .tableName(tableName)
      .key(keyFor(ReindexJob.inProgress).asJava)
      .build()

    val response = dynamoDbClient.getItem(getItemRequest)

    val item = response.item()
    if (!item.isEmpty) {
      Some(
        ReindexJob(
          status = item.get(ReindexJob.statusField).s(),
          startedAt = item.get(ReindexJob.startedAtField).s(),
          documentsExpected = item.get(ReindexJob.documentsExpectedField).n().toLong,
          documentsIndexed = item.get(ReindexJob.documentsIndexedField).n().toLong
        )
      )
    } else {
      None
    }
  }

  override def recordProgress(documentsIndexed: Long): Unit = {
    val updateItemRequest = UpdateItemRequest
      .builder()
      .tableName(tableName)
      .key(keyFor(ReindexJob.inProgress).asJava)
      .conditionExpression(s"attribute_exists(${ReindexJob.statusField})")
      .updateExpression(s"SET ${ReindexJob.documentsIndexedField} = :documentsIndexed")
      .expressionAttributeValues(
        Map(
          ":documentsIndexed" -> AttributeValue.fromN(documentsIndexed.toString)
        ).asJava
      )
      .build()
    try {
      dynamoDbClient.updateItem(updateItemRequest)
    } catch {
      case e: Exception =>
        logger.error("Failed to record progress", e)
    }
  }

  private def setCompletedJob(reindexJob: ReindexJob, setTo: String) = {
    val completedJob =
      reindexJob.copy(
        status = setTo
      )
    val putItemRequest = PutItemRequest
      .builder()
      .tableName(tableName)
      .item(itemFor(completedJob).asJava)
      .build()
    dynamoDbClient.putItem(putItemRequest)

    val deleteItemRequest = DeleteItemRequest
      .builder()
      .tableName(tableName)
      .key(keyFor(ReindexJob.inProgress).asJava)
      .build()
    dynamoDbClient.deleteItem(deleteItemRequest)
    completedJob
  }

  override def markComplete(reindexJob: ReindexJob): ReindexJob = {
    logger.info("Marking as completed")
    setCompletedJob(reindexJob, ReindexJob.completed)
  }

  override def markCancelled(reindexJob: ReindexJob): ReindexJob = {
    logger.info("Marking as cancelled")
    setCompletedJob(reindexJob, ReindexJob.cancelled)
  }

  override def markFailed(reindexJob: ReindexJob): ReindexJob = {
    logger.info("Marking as failed")
    setCompletedJob(reindexJob, ReindexJob.failed)
  }

  private def keyFor(status: String): Map[String, AttributeValue] = {
    Map(ReindexJob.statusField -> AttributeValue.fromS(status))
  }

  private def itemFor(reindexJob: ReindexJob): Map[String, AttributeValue] = {
    keyFor(reindexJob.status) ++ Map(
      ReindexJob.startedAtField -> AttributeValue.fromS(reindexJob.startedAt),
      ReindexJob.documentsExpectedField -> AttributeValue.fromN(
        reindexJob.documentsExpected.toString
      ),
      ReindexJob.documentsIndexedField -> AttributeValue.fromN(
        reindexJob.documentsIndexed.toString
      )
    )
  }
}
