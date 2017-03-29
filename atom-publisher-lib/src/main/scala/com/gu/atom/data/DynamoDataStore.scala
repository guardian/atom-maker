package com.gu.atom.data

import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.gu.atom.data.DataStore.AtomSkeleton
import com.gu.atom.data.ScanamoUtil.NestedKeyIs
import com.gu.contentatom.thrift.Atom
import com.gu.scanamo.DynamoFormat._
import com.gu.scanamo.query._
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.gu.draftcontentatom.thrift.{Atom => Draft}

abstract class DynamoDataStore
  (dynamo: AmazonDynamoDBClient, tableName: String)
    extends DataStore {

  sealed trait DynamoResult

  val atomSkeleton: AtomSkeleton[DataType]
  implicit val dynamoFormat: DynamoFormat[DataType]

  implicit class DynamoPutResult(res: PutItemResult) extends DynamoResult

  // useful shortcuts
  private val get = Scanamo.get[DataType](dynamo)(tableName) _
  private val put = Scanamo.put[DataType](dynamo)(tableName) _
  private val delete = Scanamo.delete(dynamo)(tableName) _

  private def exceptionSafePut(atom: DataType): DataStoreResult[DataType] = {
    try {
      put(atom)
      succeed(atom)
    } catch {
      case e: Exception => fail(handleException(e))
    }
  }

  private def exceptionSafeDelete(dynamoCompositeKey :DynamoCompositeKey, atom: DataType): DataStoreResult[DataType] = {
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

  def getAtom(id: String): DataStoreResult[DataType] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[DataType] =
    get(uniqueKey(dynamoCompositeKey)) match {
      case Some(Right(atom)) => Right(atom)
      case Some(Left(error)) => Left(ReadError)
      case None => Left(IDNotFound)
    }

  def createAtom(atom: DataType): DataStoreResult[DataType] = {
    val id = atomSkeleton.getId(atom)
    createAtom(DynamoCompositeKey(id), atom)
  }

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: DataType): DataStoreResult[DataType] =
    getAtom(dynamoCompositeKey) match {
      case Right(atom) =>
        fail(IDConflictError)
      case Left(error) =>
        exceptionSafePut(atom)
    }

  def deleteAtom(id: String): DataStoreResult[DataType] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[DataType] =
    getAtom(dynamoCompositeKey) match {
      case Right(atom) =>
        exceptionSafeDelete(dynamoCompositeKey, atom)
      case Left(error) =>
        fail(error)
    }

  private def findAtoms(tableName: String): DataStoreResult[List[DataType]] =
    Scanamo.scan[DataType](dynamo)(tableName).sequenceU.leftMap {
      _ => ReadError
    }

  def listAtoms: DataStoreResult[Iterator[DataType]] = findAtoms(tableName).map(_.iterator)

}

class PreviewDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)(implicit val atomSkeleton: AtomSkeleton[Atom], val dynamoFormat: DynamoFormat[Atom])
  extends DynamoDataStore(dynamo, tableName)
  with PreviewDataStore {

  def updateAtom(newAtom: Atom): DataStoreResult[Atom] = {
    val revision = newAtom.contentChangeDetails.revision
    val validationCheck = {
      NestedKeyIs(
        List('contentChangeDetails, 'revision), LT, revision
      )
    }
    val res = Scanamo.exec(dynamo)(Table[Atom](tableName).given(validationCheck).put(newAtom))
    res.fold(_ => Left(VersionConflictError(revision)), _ => Right(newAtom))
  }

}

class PublishedDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)(implicit val atomSkeleton: AtomSkeleton[Atom], val dynamoFormat: DynamoFormat[Atom])
  extends DynamoDataStore(dynamo, tableName)
  with PublishedDataStore {

  def updateAtom(newAtom: DataType) = {
    Scanamo.exec(dynamo)(Table[Atom](tableName).put(newAtom))
    succeed(newAtom)
  }
}

class DraftDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)(implicit val atomSkeleton: AtomSkeleton[Draft], val dynamoFormat: DynamoFormat[Draft])
  extends DynamoDataStore(dynamo, tableName)
    with DraftDataStore {

  def updateAtom(newAtom: Draft): DataStoreResult[Draft] = {
    Scanamo.exec(dynamo)(Table[Draft](tableName).put(newAtom))
    succeed(newAtom)
  }
}