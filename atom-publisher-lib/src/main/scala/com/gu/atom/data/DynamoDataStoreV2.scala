package com.gu.atom.data

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, ConditionalCheckFailedException, DescribeTableRequest, KeyType, ScanRequest}
import software.amazon.awssdk.awscore.exception.AwsServiceException
import com.gu.contentatom.thrift.Atom
import cats.implicits._
import io.circe._
import com.gu.atom.util.JsonSupport.backwardsCompatibleAtomDecoder
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument
import software.amazon.awssdk.enhanced.dynamodb.model.{PutItemEnhancedRequest, ScanEnhancedRequest}
import software.amazon.awssdk.enhanced.dynamodb.{AttributeConverterProvider, AttributeValueType, DynamoDbEnhancedClient, Expression, Key, TableMetadata, TableSchema}

import java.util
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala, MapHasAsJava}
import scala.util.{Failure, Success, Try}

abstract class DynamoDataStoreV2(dynamo: DynamoDbClient, tableName: String)
    extends AtomDataStore {

  private val SimpleKeyName = "id"
  private object CompositeKey {
    val partitionKey = "atomType"
    val sortKey = "id"
  }
  val desc = dynamo
    .describeTable(
      DescribeTableRequest.builder().tableName(tableName).build()
    )
    .table()

  val hasSortKey =
    desc.keySchema().asScala.exists(_.keyType() == KeyType.RANGE)

  lazy val tableSchema: TableSchema[EnhancedDocument] = {
    val builder = TableSchema
      .documentSchemaBuilder()
      .attributeConverterProviders(AttributeConverterProvider.defaultProvider())

    if (hasSortKey) {
      builder.addIndexPartitionKey(
        TableMetadata.primaryIndexName(),
        CompositeKey.partitionKey,
        AttributeValueType.S
      )
      builder.addIndexSortKey(
        TableMetadata.primaryIndexName(),
        CompositeKey.sortKey,
        AttributeValueType.S
      )
    } else
      builder.addIndexPartitionKey(
        TableMetadata.primaryIndexName(),
        SimpleKeyName,
        AttributeValueType.S
      )

    builder.build()
  }
  lazy val ddb: DynamoDbEnhancedClient =
    DynamoDbEnhancedClient.builder().dynamoDbClient(dynamo).build()

  val table = ddb.table(tableName, tableSchema)

  import AtomSerializer._

  protected def get(key: DynamoCompositeKey): DataStoreResult[Json] = {
    Try {
      Option(table.getItem(uniqueKey(key)))
    } match {
      case Success(Some(item)) => parseJson(item.toJson)
      case Success(None)       => Left(IDNotFound)
      case Failure(e)          => Left(handleException(e))
    }
  }

  protected def put(json: Json): DataStoreResult[Json] = {
    Try(
      table.putItem(
        EnhancedDocument.builder().json(json.spaces2).build()
      )
    ) match {
      case Success(_) => Right(json)
      case Failure(e) => Left(handleException(e))
    }
  }

  /** Conditional put, ensuring passed revision is higher than the value in
    * dynamo
    */
  protected def put(json: Json, revision: Long): DataStoreResult[Json] = {
    val expressionAttrValues = Map[String, AttributeValue](
      ":revision" -> AttributeValue.builder().n(revision.toString).build()
    )
    val expression = Expression
      .builder()
      .expression("contentChangeDetails.revision < :revision")
      .expressionValues(expressionAttrValues.asJava)
      .build()
    val doc = EnhancedDocument.fromJson(json.spaces2)
    val putItemRequest = PutItemEnhancedRequest
      .builder(classOf[EnhancedDocument])
      .item(doc)
      .conditionExpression(expression)
      .build()
    Try {
      table.putItem(putItemRequest)
    } match {
      case Success(item) => Right(json)
      case Failure(conditionError: ConditionalCheckFailedException) =>
        Left(VersionConflictError(revision))
      case Failure(e) => Left(handleException(e))
    }
  }

  protected def delete(key: DynamoCompositeKey): DataStoreResult[Unit] = {
    Try {
      table.deleteItem(uniqueKey(key))
    } match {
      case Success(_) => Right(())
      case Failure(e) => Left(handleException(e))
    }
  }

  protected def scan: DataStoreResult[List[Json]] = {
    Try {
      table.scan().iterator().asScala.toList
    } match {
      case Success(pages) =>
        pages
          .flatMap(p => p.items().asScala.map(i => parseJson(i.toJson)))
          .sequence
      case Failure(e) => Left(DynamoError(e.getMessage))
    }
  }

  private def uniqueKey(dynamoCompositeKey: DynamoCompositeKey): Key =
    dynamoCompositeKey match {
      case DynamoCompositeKey(partitionKey, None) =>
        Key.builder().partitionValue(partitionKey).build()

      case DynamoCompositeKey(partitionKey, Some(sortKey)) =>
        Key.builder().partitionValue(partitionKey).addSortValue(sortKey).build()
    }

  def parseJson(s: String): DataStoreResult[Json] =
    parser
      .parse(s)
      .leftMap(parsingFailure => DynamoError(parsingFailure.getMessage))

  def jsonToAtom(json: Json): DataStoreResult[Atom] =
    json
      .as[Atom](backwardsCompatibleAtomDecoder)
      .leftMap(error => DecoderError(error.message))

  private def handleException(e: Throwable) = e match {
    case serviceError: AwsServiceException =>
      DynamoError(serviceError.awsErrorDetails().errorMessage)
    case clientError: SdkException => {
      ClientError(clientError.getMessage)
    }
    case _ => ReadError
  }

  def getAtom(id: String): DataStoreResult[Atom] = getAtom(
    DynamoCompositeKey(id)
  )

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] = {
    get(dynamoCompositeKey) flatMap jsonToAtom
  }

  def createAtom(atom: Atom): DataStoreResult[Atom] =
    createAtom(DynamoCompositeKey(atom.id), atom)

  def createAtom(
      dynamoCompositeKey: DynamoCompositeKey,
      atom: Atom
  ): DataStoreResult[Atom] = {
    getAtom(dynamoCompositeKey) match {
      case Right(_) =>
        Left(IDConflictError)
      case Left(_) =>
        put(toJson(atom)).map(_ => atom)
    }
  }

  def deleteAtom(id: String): DataStoreResult[Atom] = deleteAtom(
    DynamoCompositeKey(id)
  )

  def deleteAtom(
      dynamoCompositeKey: DynamoCompositeKey
  ): DataStoreResult[Atom] =
    getAtom(dynamoCompositeKey).flatMap { atom =>
      delete(dynamoCompositeKey).map(_ => atom)
    }


  def listAtoms: DataStoreResult[List[Atom]] = scan.flatMap(_.traverse(jsonToAtom))

  override def itemCount: DataStoreResult[Long] = {
    Try {
      table.describeTable().table().itemCount()
    } match {
      case Success(count) => Right(count)
      case Failure(e) => Left(handleException(e))
    }
  }

  override def scanPage(maybeExclusiveStartKey: Option[ContinuationKey]): DataStoreResult[(List[Atom], Option[ContinuationKey])] = {
    val scanAllQuery = ScanEnhancedRequest
      .builder()
      .exclusiveStartKey(maybeExclusiveStartKey.orNull)
      .limit(100)
      .build()

    Try { table.scan(scanAllQuery).iterator().asScala.nextOption() } match {
      case Failure(error) => Left(DynamoError(error.getMessage))
      case Success(page) =>
        page match {
          case None => Right((Nil, None))
          case Some(page) =>
            val lastEvaluatedKey = Option(page.lastEvaluatedKey).filterNot(_.isEmpty)
            page
              .items()
              .asScala
              .toList
              .traverse(doc => parseJson(doc.toJson).flatMap(jsonToAtom))
              .map(atoms => (atoms, lastEvaluatedKey))
        }
    }
  }
}

class PreviewDynamoDataStoreV2(dynamo: DynamoDbClient, tableName: String)
    extends DynamoDataStoreV2(dynamo, tableName)
    with PreviewDataStore {

  import AtomSerializer._

  def updateAtom(newAtom: Atom) =
    put(toJson(newAtom), newAtom.contentChangeDetails.revision).map(_ =>
      newAtom
    )
}

class PublishedDynamoDataStoreV2(dynamo: DynamoDbClient, tableName: String)
    extends DynamoDataStoreV2(dynamo, tableName)
    with PublishedDataStore {

  import AtomSerializer._

  def updateAtom(newAtom: Atom) = put(toJson(newAtom)).map(_ => newAtom)
}
