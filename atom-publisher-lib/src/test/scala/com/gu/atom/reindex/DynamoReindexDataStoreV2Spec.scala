package com.gu.atom.reindex

import com.gu.atom.data.LocalDynamoDBV2
import com.gu.atom.util.AtomImplicitsGeneral
import org.scalatest.funspec.FixtureAnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues}
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, KeyType, PutItemRequest, ScanRequest}

import java.time.Instant
import scala.jdk.CollectionConverters._

class DynamoReindexDataStoreV2Spec
  extends FixtureAnyFunSpec
    with Matchers
    with OptionValues
    with EitherValues
    with BeforeAndAfterEach
    with AtomImplicitsGeneral {

  val client = LocalDynamoDBV2.client()
  override def beforeEach() = {
    LocalDynamoDBV2.createTable(client)(tableName)(KeyType.HASH -> "jobStatus")
  }

  override def afterEach(): Unit = {
    LocalDynamoDBV2.deleteTable(client)(tableName)
  }

  val tableName = "atom-reindex-test"
  override type FixtureParam = ReindexDataStore

  override def withFixture(test: OneArgTest) = {
    val reindexDb = new DynamoReindexDataStoreV2(LocalDynamoDBV2.client(), tableName)
    super.withFixture(
      test.toNoArgTest(reindexDb)
    )
  }

  describe("Reindex data store") {
    it("should create a reindex job entry") { reindexDb =>
      val reindexJob = reindexDb.create(5).value
      reindexJob.status should be("in progress")
      reindexJob.documentsIndexed should be(0)
      reindexJob.documentsExpected should be(5)

      val dbContents = client.scan(ScanRequest.builder().tableName(tableName).build())
      dbContents.count() should be(1)
      val dbContent = dbContents.items().asScala.head.asScala
      dbContent("jobStatus").s() should equal("in progress")
    }

    it("should fail to create a reindex job entry if another job in progress") { reindexDb =>
      val firstJob = reindexDb.create(1)
      firstJob.value.status should be ("in progress")
      firstJob.value.documentsExpected should be (1)
      val secondJob = reindexDb.create(2)
      secondJob should be(None)
      val dbContents = client.scan(ScanRequest.builder().tableName(tableName).build())
      dbContents.count() should be(1)
      val dbContent = dbContents.items().asScala.head.asScala
      dbContent("jobStatus").s() should equal("in progress")
      dbContent("documentsExpected").n() should equal ("1")
    }

    it("should clear existing completed jobs") { reindexDb =>
      client.putItem(PutItemRequest.builder().tableName(tableName).item(Map(
        "jobStatus" -> AttributeValue.fromS("completed"),
        "startedAt" -> AttributeValue.fromS(Instant.now().toEpochMilli.toString),
        "documentsIndexed" -> AttributeValue.fromN(0.toString),
        "documentsExpected" -> AttributeValue.fromN(1.toString)
      ).asJava).build())

      val newJob = reindexDb.create(2)
      newJob.value.documentsExpected should be(2)

      val dbContents = client.scan(ScanRequest.builder().tableName(tableName).build())
      dbContents.count() should be (1)
      dbContents.items().asScala.head.asScala("documentsExpected").n() should be("2")
      dbContents.items().asScala.head.asScala("jobStatus").s() should be("in progress")
    }

    it("should retrieve an existing reindex job") { reindexDb =>
      val createdJob = reindexDb.create(1)
      val retrievedJob = reindexDb.get()

      createdJob.value should equal (retrievedJob.value)
    }

    it("should retrieve in progress reindex job") { reindexDb =>
      val createdJob = reindexDb.create(1).value
      val retrievedJob = reindexDb.getInProgress()
      createdJob should equal(retrievedJob.value)

      reindexDb.markComplete(createdJob)
      val unretrievedJob = reindexDb.getInProgress()
      unretrievedJob should be (None)
    }


    it("should mark job as complete") { reindexDb =>
      val createdJob = reindexDb.create(1).value
      reindexDb.markComplete(createdJob)

      val dbContents = client.scan(ScanRequest.builder().tableName(tableName).build())
      dbContents.count() should be (1)
      dbContents.items().asScala.head.asScala("documentsExpected").n() should be("1")
      dbContents.items().asScala.head.asScala("jobStatus").s() should be("completed")
    }

    it("should mark job as cancelled") { reindexDb =>
      val createdJob = reindexDb.create(1).value
      reindexDb.markCancelled(createdJob)

      val dbContents = client.scan(ScanRequest.builder().tableName(tableName).build())
      dbContents.count() should be (1)
      dbContents.items().asScala.head.asScala("documentsExpected").n() should be("1")
      dbContents.items().asScala.head.asScala("jobStatus").s() should be("cancelled")
    }

    it("should mark job as failed") { reindexDb =>
      val createdJob = reindexDb.create(1).value
      reindexDb.markFailed(createdJob)

      val dbContents = client.scan(ScanRequest.builder().tableName(tableName).build())
      dbContents.count() should be (1)
      dbContents.items().asScala.head.asScala("documentsExpected").n() should be("1")
      dbContents.items().asScala.head.asScala("jobStatus").s() should be("failed")
    }

    it("should record progress of in progress job") { reindexDb =>
      reindexDb.create(1).value
      reindexDb.recordProgress(1)
      val retrievedJob = reindexDb.get().value
      retrievedJob.documentsIndexed should be(1)
    }

    it("should not record progress of cancelled job") { reindexDb =>
      val createdJob = reindexDb.create(1).value
      reindexDb.markCancelled(createdJob)
      reindexDb.recordProgress(2)
      val retrievedJob = reindexDb.get().value
      retrievedJob.documentsIndexed should be(0)
    }
  }

}
