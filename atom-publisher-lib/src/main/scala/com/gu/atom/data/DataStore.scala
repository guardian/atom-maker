package com.gu.atom.data

import com.gu.contentatom.thrift.Atom

sealed abstract class DataStoreError(val msg: String) extends Exception(msg)

case object IDConflictError extends DataStoreError("Atom ID already exists")
case object IDNotFound extends DataStoreError("Atom ID not in datastore")
case object ReadError extends DataStoreError("Read error")

case class  DataError(info: String) extends DataStoreError(info)
case class  VersionConflictError(requestVer: Long) extends DataStoreError(s"Update has version $requestVer, which is earlier or equal to data store version")
case class  DynamoError(info: String) extends DataStoreError(s"Dynamo was unable to process this request. Error message $info")
case class  ClientError(info: String) extends DataStoreError(s"Client was unable to get a response from a service, or if the client was unable to parse the response from a service. Error message: $info")

trait AtomDataStore extends DataStoreResultUtil {

  def getAtom(id: String): DataStoreResult[Atom]

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom]

  def createAtom(atom: Atom): DataStoreResult[Atom]

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Atom]

  def listAtoms: DataStoreResult[Iterator[Atom]]

  /* this will only allow the update if the version in atom is later
 * than the version stored in the database, otherwise it will report
 * it as a version conflict error */
  def updateAtom(newAtom: Atom): DataStoreResult[Atom]

  def deleteAtom(id: String): DataStoreResult[Atom]

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom]
}

trait DataStoreResultUtil {
  type DataStoreResult[R] = Either[DataStoreError, R]

  def fail(error: DataStoreError): DataStoreResult[Nothing] = Left(error)
  def succeed[R](result: => R): DataStoreResult[R] = Right(result)
}

object DataStoreResultUtil extends DataStoreResultUtil

trait PreviewDataStore extends AtomDataStore

trait PublishedDataStore extends AtomDataStore

case class DynamoCompositeKey(partitionKey: String, sortKey: Option[String] = None)
