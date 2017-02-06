package com.gu.atom.data

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, DeleteItemResult, PutItemResult}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.gu.contentatom.thrift.{Atom, AtomData, Flags}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import com.gu.scanamo.query._
import cats.instances.either._
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._

import scala.reflect.ClassTag
import com.twitter.scrooge.ThriftStruct
import DynamoFormat._
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import AtomData._
import com.gu.atom.data._
import ScanamoUtil._

import scala.util.Success

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

  def createAtom(atom: Atom): DataStoreResult[Unit] = createAtom(DynamoCompositeKey(atom.id), atom)

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Unit] =
    if (get(uniqueKey(dynamoCompositeKey)).isDefined)
      fail(IDConflictError)
    else
      succeed(put(atom))

  private def findAtoms(tableName: String): DataStoreResult[List[Atom]] =
    Scanamo.scan[Atom](dynamo)(tableName).sequenceU.leftMap {
      _ => ReadError
    }

  def listAtoms: DataStoreResult[Iterator[Atom]] = findAtoms(tableName).map(_.iterator)

  def deleteAtom(id: String): DataStoreResult[Unit] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Unit] = {
    getAtom(dynamoCompositeKey) match {
      case Right(_) => succeed(delete(uniqueKey(dynamoCompositeKey)))
      case Left(error) => fail(error)
    }
  }
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
    res.map(_ => ())
      .leftMap(_ => VersionConflictError(newAtom.contentChangeDetails.revision))
  }

}

abstract class PublishedDynamoDataStore[D : ClassTag : DynamoFormat]
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore[D](dynamo, tableName)
  with PublishedDataStore {

  def updateAtom(newAtom: Atom) =
    succeed(Scanamo.exec(dynamo)(Table[Atom](tableName).put(newAtom)))
}
