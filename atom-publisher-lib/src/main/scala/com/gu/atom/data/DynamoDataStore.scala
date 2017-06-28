package com.gu.atom.data

import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.gu.atom.data.ScanamoUtil._
import com.gu.atom.facade.AtomFacade
import com.gu.atom.util.ThriftDynamoFormat
import com.gu.contentatom.thrift.Atom
import com.gu.scanamo.DynamoFormat._
import com.gu.scanamo.query._
import com.gu.scanamo.{Scanamo, Table}
import com.gu.scanamo.error.DynamoReadError

abstract class DynamoDataStore
  (dynamo: AmazonDynamoDBClient, tableName: String)
    extends AtomDataStore with AtomDynamoFormats {
  import AtomFacade._
  import ThriftDynamoFormat._

  sealed trait DynamoResult

  implicit class DynamoPutResult(res: PutItemResult) extends DynamoResult

  // useful shortcuts
  private def get(key: UniqueKey[_]) =
    Scanamo.get[Atom](dynamo)(tableName)(key)(atomFormat)
  private def put(item: Atom) =
    Scanamo.put[Atom](dynamo)(tableName)(item)(atomFormat)
  private val delete = Scanamo.delete(dynamo)(tableName) _

  private def exceptionSafePut(atom: Atom): DataStoreResult[Atom] = {
    try {
      put(atom)
      succeed(atom)
    } catch {
      case e: Exception => fail(handleException(e))
    }
  }

  private def exceptionSafeDelete(dynamoCompositeKey :DynamoCompositeKey, atom: Atom): DataStoreResult[Atom] = {
    try {
      delete(uniqueKey(dynamoCompositeKey))
      succeed(atom)
    } catch {
      case e: Exception => fail(handleException(e))
    }
  }

  private def handleException(e: Exception) = e match {
    case serviceError: AmazonServiceException => DynamoError(serviceError.getErrorMessage)
    case clientError: AmazonClientException => ClientError(clientError.getMessage)
    case other => ReadError(e.getMessage)
  }

  private def uniqueKey(dynamoCompositeKey: DynamoCompositeKey) = dynamoCompositeKey match {
    case DynamoCompositeKey(partitionKey, None) => UniqueKey(KeyEquals('id, partitionKey))
    case DynamoCompositeKey(partitionKey, Some(sortKey)) => UniqueKey(KeyEquals('atomType, partitionKey) and KeyEquals('id, sortKey))
  }

  def getAtom(id: String): DataStoreResult[Atom] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    get(uniqueKey(dynamoCompositeKey)) match {
      case Some(Right(atom)) => Right(atom)
      case Some(Left(error)) => Left(ReadError(DynamoReadError.describe(error)))
      case None => Left(IDNotFound)
    }

  def createAtom(atom: Atom): DataStoreResult[Atom] = createAtom(DynamoCompositeKey(atom.id), atom)

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Atom] =
    getAtom(dynamoCompositeKey) match {
      case Right(atom) =>
        fail(IDConflictError)
      case Left(error) =>
        exceptionSafePut(atom)
    }

  def deleteAtom(id: String): DataStoreResult[Atom] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    getAtom(dynamoCompositeKey) match {
      case Right(atom) =>
        exceptionSafeDelete(dynamoCompositeKey, atom)
      case Left(error) =>
        fail(error)
    }

  private def findAtoms(tableName: String): DataStoreResult[List[Atom]] =
    Scanamo.scan[Atom](dynamo)(tableName)(atomFormat).sequenceU.leftMap {
      err => ReadError(DynamoReadError.describe(err))
    }

  def listAtoms: DataStoreResult[Iterator[Atom]] = findAtoms(tableName).map(_.iterator)

}

class PreviewDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore(dynamo, tableName)
  with PreviewDataStore {

  def updateAtom(newAtom: Atom) = {
    val validationCheck = NestedKeyIs(
      List('contentChangeDetails, 'revision), LT, newAtom.contentChangeDetails.revision
    )
    val res = Scanamo.exec(dynamo)(Table[Atom](tableName)(atomFormat).given(validationCheck).put(newAtom))
    res.fold(_ => Left(VersionConflictError(newAtom.contentChangeDetails.revision)), _ => Right(newAtom))
  }

}

class PublishedDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore(dynamo, tableName)
  with PublishedDataStore {

  def updateAtom(newAtom: Atom) = {
    Scanamo.exec(dynamo)(Table[Atom](tableName)(atomFormat).put(newAtom))
    succeed(newAtom)
  }
}
