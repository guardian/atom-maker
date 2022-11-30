package com.gu.atom.data

import com.gu.contentatom.thrift.Atom

sealed abstract class DataStoreError(val msg: String) extends Exception(msg)

case object IDConflictError extends DataStoreError("Atom ID already exists")
case object IDNotFound extends DataStoreError("Atom ID not in datastore")
case object ReadError extends DataStoreError("Read error")

case class  VersionConflictError(requestVer: Long) extends DataStoreError(s"Update has version $requestVer, which is earlier or equal to data store version")
case class  DynamoError(info: String) extends DataStoreError(s"Dynamo was unable to process this request. Error message $info")
case class  ClientError(info: String) extends DataStoreError(s"Client was unable to get a response from a service, or if the client was unable to parse the response from a service. Error message: $info")
case class  DecoderError(info: String) extends DataStoreError(s"Error decoding json to atom: $info")

trait AtomDataStore[ATOM <: Atom] extends DataStoreResultUtil {

  def getAtom(id: String): DataStoreResult[ATOM]

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[ATOM]

  def createAtom(atom: ATOM): DataStoreResult[ATOM]

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: ATOM): DataStoreResult[ATOM]

  def listAtoms: DataStoreResult[List[ATOM]]

  /* this will only allow the update if the version in atom is later
 * than the version stored in the database, otherwise it will report
 * it as a version conflict error */
  def updateAtom(newAtom: ATOM): DataStoreResult[ATOM]

  def deleteAtom(id: String): DataStoreResult[ATOM]

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[ATOM]
}

trait DataStoreResultUtil {
  type DataStoreResult[R] = Either[DataStoreError, R]

  def fail(error: DataStoreError): DataStoreResult[Nothing] = Left(error)
  def succeed[R](result: => R): DataStoreResult[R] = Right(result)
}

object DataStoreResultUtil extends DataStoreResultUtil

trait PreviewDataStore[ATOM <: Atom] extends AtomDataStore[ATOM]

trait PublishedDataStore[ATOM <: Atom] extends AtomDataStore[ATOM]

case class DynamoCompositeKey(partitionKey: String, sortKey: Option[String] = None)
