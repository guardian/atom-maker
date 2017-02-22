package com.gu.atom.data

import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.gu.scanamo.query._
import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._

import scala.reflect.ClassTag
import DynamoFormat._
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import ScanamoUtil._
import com.amazonaws.{AmazonClientException, AmazonServiceException}

abstract class DynamoDataStore[D : ClassTag : DynamoFormat]
  (dynamo: AmazonDynamoDBClient, tableName: String)
    extends DataStore
    with AtomDynamoFormats[D] {

  sealed trait DynamoResult

  implicit class DynamoPutResult(res: PutItemResult) extends DynamoResult

  // useful shortcuts
  private val get = Scanamo.get[Atom](dynamo)(tableName) _
  private val put = Scanamo.put[Atom](dynamo)(tableName) _
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
    case _ => ReadError
  }

  private def uniqueKey(dynamoCompositeKey: DynamoCompositeKey) = dynamoCompositeKey match {
    case DynamoCompositeKey(partitionKey, None) => UniqueKey(KeyEquals('id, partitionKey))
    case DynamoCompositeKey(partitionKey, Some(sortKey)) => UniqueKey(KeyEquals('atomType, partitionKey) and KeyEquals('id, sortKey))
  }

  def getAtom(id: String): DataStoreResult[Atom] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    get(uniqueKey(dynamoCompositeKey)) match {
      case Some(Right(atom)) => Right(atom)
      case Some(Left(error)) => Left(ReadError)
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
    Scanamo.scan[Atom](dynamo)(tableName).sequenceU.leftMap {
      _ => ReadError
    }

  def listAtoms: DataStoreResult[Iterator[Atom]] = findAtoms(tableName).map(_.iterator)

}

abstract class PreviewDynamoDataStore[D : ClassTag : DynamoFormat]
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore[D](dynamo, tableName)
  with PreviewDataStore {

  def updateAtom(newAtom: Atom) = {
    val validationCheck = NestedKeyIs(
      List('contentChangeDetails, 'revision), LT, newAtom.contentChangeDetails.revision
    )
    val res = Scanamo.exec(dynamo)(Table[Atom](tableName).given(validationCheck).put(newAtom))
    res.fold(_ => Left(VersionConflictError(newAtom.contentChangeDetails.revision)), _ => Right(newAtom))
  }

}

abstract class PublishedDynamoDataStore[D : ClassTag : DynamoFormat]
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore[D](dynamo, tableName)
  with PublishedDataStore {

  def updateAtom(newAtom: Atom) = {
    Scanamo.exec(dynamo)(Table[Atom](tableName).put(newAtom))
    succeed(newAtom)
  }
}
