package com.gu.atom.reindex

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class ReindexJob(
  status: String,
  startedAt: String,
  documentsExpected: Long,
  documentsIndexed: Long
)

object ReindexJob {
  implicit val decoder: Decoder[ReindexJob] = deriveDecoder[ReindexJob]
  implicit val encoder: Encoder[ReindexJob] = deriveEncoder[ReindexJob]

  val inProgress = "in progress"
  val completed = "completed"
  val failed = "failed"
  val cancelled = "cancelled"
  val completedJobStatuses = Seq(completed, failed, cancelled)

  val statusField =
    "jobStatus" // 'status' is a reserved dynamo keyword
  val startedAtField = "startedAt"
  val documentsExpectedField = "documentsExpected"
  val documentsIndexedField = "documentsIndexed"
}