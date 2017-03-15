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
import com.gu.scanamo.DynamoFormat._
import com.gu.scanamo.query._
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}

abstract class DynamoDataStore[A: DynamoFormat]
  (dynamo: AmazonDynamoDBClient, tableName: String)(implicit val atomSkeleton: AtomSkeleton[A])
    extends DataStore[A] {

  sealed trait DynamoResult

  implicit class DynamoPutResult(res: PutItemResult) extends DynamoResult

  // useful shortcuts
  private val get = Scanamo.get[A](dynamo)(tableName) _
  private val put = Scanamo.put[A](dynamo)(tableName) _
  private val delete = Scanamo.delete(dynamo)(tableName) _

  private def exceptionSafePut(atom: A): DataStoreResult[A] = {
    try {
      put(atom)
      succeed(atom)
    } catch {
      case e: Exception => fail(handleException(e))
    }
  }

  private def exceptionSafeDelete(dynamoCompositeKey :DynamoCompositeKey, atom: A): DataStoreResult[A] = {
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

  def getAtom(id: String): DataStoreResult[A] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[A] =
    get(uniqueKey(dynamoCompositeKey)) match {
      case Some(Right(atom)) => Right(atom)
      case Some(Left(error)) => Left(ReadError)
      case None => Left(IDNotFound)
    }

  def createAtom(atom: A): DataStoreResult[A] = {
    val id = atomSkeleton.getId(atom)
    createAtom(DynamoCompositeKey(id), atom)
  }

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: A): DataStoreResult[A] =
    getAtom(dynamoCompositeKey) match {
      case Right(atom) =>
        fail(IDConflictError)
      case Left(error) =>
        exceptionSafePut(atom)
    }

  def deleteAtom(id: String): DataStoreResult[A] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[A] =
    getAtom(dynamoCompositeKey) match {
      case Right(atom) =>
        exceptionSafeDelete(dynamoCompositeKey, atom)
      case Left(error) =>
        fail(error)
    }

  private def findAtoms(tableName: String): DataStoreResult[List[A]] =
    Scanamo.scan[A](dynamo)(tableName).sequenceU.leftMap {
      _ => ReadError
    }

  def listAtoms: DataStoreResult[Iterator[A]] = findAtoms(tableName).map(_.iterator)

}

class PreviewDynamoDataStore[A: DynamoFormat]
(dynamo: AmazonDynamoDBClient, tableName: String)(implicit override val atomSkeleton: AtomSkeleton[A])
  extends DynamoDataStore[A](dynamo, tableName)
  with PreviewDataStore[A] {

  def updateAtom(newAtom: A): DataStoreResult[A] = {
    Left(VersionConflictError(3))

    val revision = atomSkeleton.getContentChangeDetails(newAtom).revision
    val validationCheck = {
      NestedKeyIs(
        List('contentChangeDetails, 'revision), LT, revision
      )
    }
    val res = Scanamo.exec(dynamo)(Table[A](tableName).given(validationCheck).put(newAtom))
    res.fold(_ => Left(VersionConflictError(revision)), _ => Right(newAtom))
  }

}

class PublishedDynamoDataStore[A: DynamoFormat]
(dynamo: AmazonDynamoDBClient, tableName: String)(implicit override val atomSkeleton: AtomSkeleton[A])
  extends DynamoDataStore[A](dynamo, tableName)
  with PublishedDataStore[A] {

  def updateAtom(newAtom: A) = {
    Scanamo.exec(dynamo)(Table[A](tableName).put(newAtom))
    succeed(newAtom)
  }
}
