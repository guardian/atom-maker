package com.gu.atom.data

import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.model.{ConditionalCheckFailedException, DeleteItemResult}
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.gu.contentatom.thrift.Atom
import cats.implicits._
import cats.syntax.either._
import io.circe._
import io.circe.syntax._
import com.gu.fezziwig.CirceScroogeMacros.{encodeThriftStruct, encodeThriftUnion}
import com.gu.atom.util.JsonSupport.{backwardsCompatibleAtomDecoder, thriftEnumEncoder}

import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

abstract class DynamoDataStore
  (dynamo: AmazonDynamoDBClient, tableName: String)
    extends AtomDataStore {

  private val dynamoDB = new DynamoDB(dynamo)
  private val table = dynamoDB.getTable(tableName)

  private val SimpleKeyName = "id"
  private object CompositeKey {
    val partitionKey = "atomType"
    val sortKey = "id"
  }

  protected def get(key: DynamoCompositeKey): DataStoreResult[Json] = {
    Try {
      val result = table.getItem(uniqueKey(key))
      Option(result)  //null if not found

    } match {
      case Success(Some(item)) => parseJson(item.toJSON)
      case Success(None) => Left(IDNotFound)
      case Failure(e) => Left(handleException(e))
    }
  }

  protected def put(json: Json): DataStoreResult[Json] = {
    Try(table.putItem(jsonToItem(json))) match {
      case Success(_) => Right(json)
      case Failure(e) => Left(handleException(e))
    }
  }

  /**
    * Conditional put, ensuring passed revision is higher than the value in dynamo
    */
  protected def put(json: Json, revision: Long): DataStoreResult[Json] = {
    val expressionAttributeValues = new util.HashMap[String, Object]()
    expressionAttributeValues.put(":revision", revision.asInstanceOf[Object])

    Try {
      table.putItem(
        jsonToItem(json),
        "contentChangeDetails.revision < :revision",
        null,
        expressionAttributeValues
      )
    } match {
      case Success(_) => Right(json)
      case Failure(conditionError: ConditionalCheckFailedException) =>
        Left(VersionConflictError(revision))
      case Failure(e) => Left(handleException(e))
    }
  }

  protected def delete(key: DynamoCompositeKey): DataStoreResult[DeleteItemResult] = {
    Try {
      key match {
        case DynamoCompositeKey(partitionKey, None) =>
          table.deleteItem(SimpleKeyName, partitionKey)
        case DynamoCompositeKey(partitionKey, Some(sortKey)) =>
          table.deleteItem(CompositeKey.partitionKey, partitionKey, CompositeKey.sortKey, sortKey)
      }
    } match {
      case Success(outcome) => Right(outcome.getDeleteItemResult)
      case Failure(e) => Left(handleException(e))
    }
  }

  protected def scan: DataStoreResult[List[Json]] = {
    Try {
      table.scan().iterator.asScala.toList

    } match {
      case Success(items) => items.traverse(item => parseJson(item.toJSON))
      case Failure(e) => Left(DynamoError(e.getMessage))
    }
  }

  private def uniqueKey(dynamoCompositeKey: DynamoCompositeKey) = dynamoCompositeKey match {
    case DynamoCompositeKey(partitionKey, None) =>
      new PrimaryKey(SimpleKeyName, partitionKey)
    case DynamoCompositeKey(partitionKey, Some(sortKey)) =>
      new PrimaryKey(CompositeKey.partitionKey, partitionKey, CompositeKey.sortKey, sortKey)
  }

  def parseJson(s: String): DataStoreResult[Json] =
    parser.parse(s).leftMap(parsingFailure => DynamoError(parsingFailure.getMessage))

  def jsonToAtom(json: Json): DataStoreResult[Atom] =
    json.as[Atom](backwardsCompatibleAtomDecoder).leftMap(error => DecoderError(error.message))

  def jsonToItem(json: Json): Item = {
    val item = new Item()
    json.asObject.foreach { obj =>
      obj.toMap.map { case (key, value) => item.withJSON(key, value.noSpaces) }
    }
    item
  }

  private def handleException(e: Throwable) = e match {
    case serviceError: AmazonServiceException => DynamoError(serviceError.getErrorMessage)
    case clientError: AmazonClientException => ClientError(clientError.getMessage)
    case other => ReadError
  }

  def getAtom(id: String): DataStoreResult[Atom] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    get(dynamoCompositeKey) flatMap jsonToAtom

  def createAtom(atom: Atom): DataStoreResult[Atom] = createAtom(DynamoCompositeKey(atom.id), atom)

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Atom] = {
    getAtom(dynamoCompositeKey) match {
      case Right(_) =>
        Left(IDConflictError)
      case Left(error) =>
        put(atom.asJson).map(_ => atom)
    }
  }

  def deleteAtom(id: String): DataStoreResult[Atom] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    getAtom(dynamoCompositeKey).flatMap { atom =>
      delete(dynamoCompositeKey).map(_ => atom)
    }

  private def findAtoms(tableName: String): DataStoreResult[List[Atom]] = {
    scan.flatMap { jsonItems =>
      val atomDecoderResults = jsonItems.map { json =>
        jsonToAtom(json)
      }
      atomDecoderResults.sequence
    }
  }

  def listAtoms: DataStoreResult[List[Atom]] = findAtoms(tableName)

}

class PreviewDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore(dynamo, tableName)
  with PreviewDataStore {

  def updateAtom(newAtom: Atom) =
    put(newAtom.asJson, newAtom.contentChangeDetails.revision).map(_ => newAtom)
}

class PublishedDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore(dynamo, tableName)
  with PublishedDataStore {

  def updateAtom(newAtom: Atom) = put(newAtom.asJson).map(_ => newAtom)
}
