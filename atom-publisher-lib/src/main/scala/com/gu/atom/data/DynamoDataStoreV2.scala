package com.gu.atom.data

import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbClient}
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, DeleteItemResponse, DescribeTableRequest, GetItemRequest, ItemResponse}
import software.amazon.awssdk.awscore.exception.AwsServiceException
import com.gu.contentatom.thrift.Atom
import cats.implicits._
import io.circe._
import com.gu.atom.util.JsonSupport.backwardsCompatibleAtomDecoder
import io.circe.syntax.EncoderOps
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata.primaryIndexName
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.{AttributeConverterProvider, AttributeValueType, DynamoDbEnhancedClient, Key, TableMetadata, TableSchema}

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}

abstract class DynamoDataStoreV2
  (dynamo: DynamoDbClient, tableName: String)
    extends AtomDataStore {

  lazy val ddb: DynamoDbEnhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamo).build()

  lazy val tableSchema1 = TableSchema.documentSchemaBuilder()
    .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
    .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
    .build()

  val table1 = ddb.table(tableName, tableSchema1)

  lazy val tableSchema2 = TableSchema.documentSchemaBuilder()
    .addIndexPartitionKey(TableMetadata.primaryIndexName(), CompositeKey.partitionKey, AttributeValueType.S)
    .addIndexSortKey("commission-index", CompositeKey.sortKey, AttributeValueType.S)
    .attributeConverterProviders(AttributeConverterProvider.defaultProvider())
    .build()

  val table2 = ddb.table(tableName, tableSchema2)

  private val SimpleKeyName = "id"
  private object CompositeKey {
    val partitionKey = "atomType"
    val sortKey = "id"
  }

  import AtomSerializer._

  protected def get(key: DynamoCompositeKey): DataStoreResult[Json] = {
    Try {
      Option(getTableToQuery(key).getItem(uniqueKey(key)))
    } match {
      case Success(Some(item)) => parseJson(item.toJson)
      case Success(None) => Left(IDNotFound)
      case Failure(e) => Left(handleException(e))
    }


  }

  protected def put(json: Json): DataStoreResult[Json] = {
//    Try(table.putItem(jsonToItem(json))) match {
//      case Success(_) => Right(json)
//      case Failure(e) => Left(handleException(e))
//    }
    ???
  }

  /**
    * Conditional put, ensuring passed revision is higher than the value in dynamo
    */
  protected def put(json: Json, revision: Long): DataStoreResult[Json] = {
//    val expressionAttributeValues = new util.HashMap[String, Object]()
//    expressionAttributeValues.put(":revision", revision.asInstanceOf[Object])
//
//    Try {
//      table.putItem(
//        jsonToItem(json),
//        "contentChangeDetails.revision < :revision",
//        null,
//        expressionAttributeValues
//      )
//    } match {
//      case Success(_) => Right(json)
//      case Failure(conditionError: ConditionalCheckFailedException) =>
//        Left(VersionConflictError(revision))
//      case Failure(e) => Left(handleException(e))
//    }
    ???
  }

  protected def delete(key: DynamoCompositeKey): DataStoreResult[DeleteItemResponse] = {
//    Try {
//      key match {
//        case DynamoCompositeKey(partitionKey, None) =>
//          table.deleteItem(SimpleKeyName, partitionKey)
//        case DynamoCompositeKey(partitionKey, Some(sortKey)) =>
//          table.deleteItem(CompositeKey.partitionKey, partitionKey, CompositeKey.sortKey, sortKey)
//      }
//    } match {
//      case Success(outcome) => Right(outcome.getDeleteItemResult)
//      case Failure(e) => Left(handleException(e))
//    }
    ???
  }

  protected def scan: DataStoreResult[List[Json]] = {
//    Try {
//      table.scan().iterator.asScala.toList
//
//    } match {
//      case Success(items) => items.traverse(item => parseJson(item.toJSON))
//      case Failure(e) => Left(DynamoError(e.getMessage))
//    }
    ???
  }


//  private def uniqueKey(dynamoCompositeKey: DynamoCompositeKey): Map[String, AttributeValue] = dynamoCompositeKey match {
//    case DynamoCompositeKey(partitionKey, None) => {
//      Map(SimpleKeyName -> AttributeValue.builder().s(partitionKey).build())
//    }
//
//    case DynamoCompositeKey(partitionKey, Some(sortKey)) =>{
//      Map(
//        CompositeKey.partitionKey -> AttributeValue.builder().s(partitionKey).build(),
//        CompositeKey.sortKey -> AttributeValue.builder().s(sortKey).build()
//      )
//    }
//  }
  private def uniqueKey(dynamoCompositeKey: DynamoCompositeKey): Key = dynamoCompositeKey match {
    case DynamoCompositeKey(partitionKey, None) => {
      Key.builder().partitionValue(partitionKey).build()
    }

    case DynamoCompositeKey(partitionKey, Some(sortKey)) =>{
      Key.builder().partitionValue(partitionKey).addSortValue(sortKey).build()
    }
  }

  private def getTableToQuery(dynamoCompositeKey: DynamoCompositeKey) = dynamoCompositeKey match {
    case DynamoCompositeKey(_, None) => table1
    case DynamoCompositeKey(_, Some(_)) => table2
  }

  def parseJson(s: String): DataStoreResult[Json] =
    parser.parse(s).leftMap(parsingFailure => DynamoError(parsingFailure.getMessage))

  def jsonToAtom(json: Json): DataStoreResult[Atom] =
    json.as[Atom](backwardsCompatibleAtomDecoder).leftMap(error => DecoderError(error.message))

  def jsonToItem(json: Json): ItemResponse = {
//    val item = new Item()
//    json.asObject.foreach { obj =>
//      obj.toMap.map { case (key, value) => item.withJSON(key, value.noSpaces) }
//    }
//    item
    ???
  }

  private def handleException(e: Throwable) = e match {
    case serviceError: AwsServiceException => DynamoError(serviceError.awsErrorDetails().errorMessage)
    case clientError: SdkException => {
      ClientError(clientError.getMessage)
    }
    case other => ReadError
  }

  def getAtom(id: String): DataStoreResult[Atom] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] = {
    get(dynamoCompositeKey) flatMap jsonToAtom
  }

  def createAtom(atom: Atom): DataStoreResult[Atom] = createAtom(DynamoCompositeKey(atom.id), atom)

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Atom] = {
    getAtom(dynamoCompositeKey) match {
      case Right(_) =>
        Left(IDConflictError)
      case Left(error) =>
        put(toJson(atom)).map(_ => atom)
    }
  }

  def deleteAtom(id: String): DataStoreResult[Atom] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    getAtom(dynamoCompositeKey).flatMap { atom =>
      delete(dynamoCompositeKey).map(_ => atom)
    }

  private def findAtoms(tableName: String): DataStoreResult[List[Atom]] =
    scan.flatMap(_.traverse(jsonToAtom))

  def listAtoms: DataStoreResult[List[Atom]] = findAtoms(tableName)

}

class PreviewDynamoDataStoreV2
(dynamo: DynamoDbClient, tableName: String)
  extends DynamoDataStoreV2(dynamo, tableName)
  with PreviewDataStore {

  import AtomSerializer._

  def updateAtom(newAtom: Atom) =
    put(toJson(newAtom), newAtom.contentChangeDetails.revision).map(_ => newAtom)
}

class PublishedDynamoDataStoreV2
(dynamo: DynamoDbClient, tableName: String)
  extends DynamoDataStoreV2(dynamo, tableName)
  with PublishedDataStore {

  import AtomSerializer._

  def updateAtom(newAtom: Atom) = put(toJson(newAtom)).map(_ => newAtom)
}

