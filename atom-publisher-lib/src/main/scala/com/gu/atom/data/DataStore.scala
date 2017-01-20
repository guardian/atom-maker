package com.gu.atom.data

import cats.syntax.either._
import com.gu.contentatom.thrift.Atom

import com.typesafe.scalalogging.LazyLogging

sealed abstract class DataStoreError(val msg: String) extends Exception(msg)

case object IDConflictError extends DataStoreError("Atom ID already exists")
case object IDNotFound extends DataStoreError("Atom ID not in datastore")
case object ReadError extends DataStoreError("Read error")

case class  DataError(info: String) extends DataStoreError(info)
case class  VersionConflictError(requestVer: Long)
    extends DataStoreError(s"Update has version $requestVer, which is earlier or equal to data store version")

trait DataStore extends DataStoreResult {

  def getAtom(id: String): Option[Atom]

  def createAtom(atom: Atom): DataStoreResult[Unit]

  /* this will only allow the update if the version in atom is later
   * than the version stored in the database, otherwise it will report
   * it as a version conflict error */

  def listAtoms: DataStoreResult[Iterator[Atom]]

  def updateAtom(newAtom: Atom): DataStoreResult[Unit]
}

trait DataStoreResult {
  type DataStoreResult[R] = Either[DataStoreError, R]

  def fail(error: DataStoreError): DataStoreResult[Nothing] = Left(error)
  def succeed[R](result: => R): DataStoreResult[R] = Right(result)
}

object DataStoreResult extends DataStoreResult

trait PreviewDataStore extends DataStore

trait PublishedDataStore extends DataStore