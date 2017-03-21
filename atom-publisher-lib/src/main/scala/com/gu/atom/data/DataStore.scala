package com.gu.atom.data

import com.gu.contentatom.thrift.{Atom, ContentChangeDetails}
import simulacrum.typeclass

sealed abstract class DataStoreError(val msg: String) extends Exception(msg)

case object IDConflictError extends DataStoreError("Atom ID already exists")
case object IDNotFound extends DataStoreError("Atom ID not in datastore")
case object ReadError extends DataStoreError("Read error")

case class  DataError(info: String) extends DataStoreError(info)
case class  VersionConflictError(requestVer: Long) extends DataStoreError(s"Update has version $requestVer, which is earlier or equal to data store version")
case class  DynamoError(info: String) extends DataStoreError(s"Dynamo was unable to process this request. Error message $info")
case class  ClientError(info: String) extends DataStoreError(s"Client was unable to get a response from a service, or if the client was unable to parse the response from a service. Error message: $info")

case class Draft(id: String, contentChangeDetails: ContentChangeDetails)

trait DataStore extends DataStoreResult {

  type DataType

  def getAtom(id: String): DataStoreResult[DataType]

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[DataType]

  def createAtom(atom: DataType): DataStoreResult[DataType]

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: DataType): DataStoreResult[DataType]

  def listAtoms: DataStoreResult[Iterator[DataType]]

  /* this will only allow the update if the version in atom is later
 * than the version stored in the database, otherwise it will report
 * it as a version conflict error */
  def updateAtom(newAtom: DataType): DataStoreResult[DataType]

  def deleteAtom(id: String): DataStoreResult[DataType]

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[DataType]
}

object DataStore {
  @typeclass
  trait AtomSkeleton[T] {
    def getId(t: T): String
    def getContentChangeDetails(t: T): ContentChangeDetails
  }

  implicit val CompleteAtom: AtomSkeleton[Atom] = new AtomSkeleton[Atom] {
    override def getId(t: Atom) = t.id

    override def getContentChangeDetails(t: Atom): ContentChangeDetails = t.contentChangeDetails
  }

  implicit val DraftAtom: AtomSkeleton[Draft] = new AtomSkeleton[Draft] {
    override def getId(t: Draft) = t.id

    override def getContentChangeDetails(t: Draft): ContentChangeDetails = t.contentChangeDetails
  }
}

trait DataStoreResult {
  type DataStoreResult[R] = Either[DataStoreError, R]

  def fail(error: DataStoreError): DataStoreResult[Nothing] = Left(error)
  def succeed[R](result: => R): DataStoreResult[R] = Right(result)
}

object DataStoreResult extends DataStoreResult

trait PreviewDataStore extends DataStore {
  type DataType = Atom
}

trait PublishedDataStore extends DataStore {
  type DataType = Atom
}

trait DraftDataStore extends DataStore {
  type DataType = Draft
}

case class DynamoCompositeKey(partitionKey: String, sortKey: Option[String] = None)