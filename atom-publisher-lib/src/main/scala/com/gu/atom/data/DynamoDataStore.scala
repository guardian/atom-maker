package com.gu.atom.data

import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemResult}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.gu.contentatom.thrift.AtomData.{Cta, Media}
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.gu.scanamo.error.{TypeCoercionError, DynamoReadError}
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

abstract class DynamoDataStore
  (dynamo: AmazonDynamoDBClient, tableName: String)
    extends DataStore {

//  implicit val atomDataFormat: DynamoFormat[AtomData] =  new DynamoFormat[AtomData] {
//
//    val mediaFormat: DynamoFormat[Media] = new DynamoFormat[Media] {
//      private def fromAtomData: PartialFunction[AtomData, MediaAtom] = { case AtomData.Media(data) => data }
//      private def toAtomData(data: MediaAtom): AtomData = AtomData.Media(data)
//
//      def write(atomData: Media): AttributeValue = {
//        val pf = fromAtomData andThen { case data: Media => arg0.write(data) }
//        pf.applyOrElse(atomData, fallback)
//      }
//
//      def read(attr: AttributeValue): Either[DynamoReadError, AtomData] = read(attr).map(x => toAtomData(x.asInstanceOf[MediaAtom]))
//    }

  def mediaFormat(implicit arg0: DynamoFormat[MediaAtom]): DynamoFormat[AtomData] = {
    def fromAtomData: PartialFunction[AtomData, MediaAtom] = { case AtomData.Media(data) => data }
    def toAtomData(data: MediaAtom): AtomData = AtomData.Media(data)
    def fallback(atomData: AtomData): AttributeValue =
      new AttributeValue().withS(s"unknown atom data type $atomData")

    new DynamoFormat[AtomData] {
      def write(atomData: AtomData): AttributeValue = {
        val pf = fromAtomData andThen { case data: MediaAtom => arg0.write(data) }
        pf.applyOrElse(atomData, fallback)
      }

      def read(attr: AttributeValue) = arg0.read(attr) map toAtomData
    }
  }

  def ctaFormat(implicit arg0: DynamoFormat[CTAAtom]): DynamoFormat[AtomData] = {
    def fromAtomData: PartialFunction[AtomData, CTAAtom] = { case AtomData.Cta(data) => data }
    def toAtomData(data: CTAAtom): AtomData = AtomData.Cta(data)
    def fallback(atomData: AtomData): AttributeValue =
      new AttributeValue().withS(s"unknown atom data type $atomData")

    new DynamoFormat[AtomData] {
      def write(atomData: AtomData): AttributeValue = {
        val pf = fromAtomData andThen { case data: CTAAtom => arg0.write(data) }
        pf.applyOrElse(atomData, fallback)
      }

      def read(attr: AttributeValue) = arg0.read(attr) map toAtomData
    }
  }

  val allFormats: List[DynamoFormat[AtomData]] = List(mediaFormat, ctaFormat)

  implicit val dynamoFormat: DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(t: AtomData) = t match {
      case Media(_) => mediaFormat.write(t)
      case Cta(_) => ctaFormat.write(t)
    }

    def read(av: AttributeValue): Either[DynamoReadError, AtomData] = {
      allFormats.map(_.read(av)).collectFirst { case succ@Right(_) => succ }.getOrElse(Left(TypeCoercionError(new RuntimeException(s"No dynamo format to read $av"))))
    }
  }

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

class PreviewDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore(dynamo, tableName)
  with PreviewDataStore {

  def updateAtom(newAtom: Atom) = {
    val validationCheck = NestedKeyIs(
      List('contentChangeDetails, 'revision), LT, newAtom.contentChangeDetails.revision
    )
    val res = Scanamo.exec(dynamo)(Table[Atom](tableName).given(validationCheck).put(newAtom))
    res.fold(_ => Left(VersionConflictError(newAtom.contentChangeDetails.revision)), _ => Right(newAtom))
  }

}

class PublishedDynamoDataStore
(dynamo: AmazonDynamoDBClient, tableName: String)
  extends DynamoDataStore(dynamo, tableName)
  with PublishedDataStore {

  def updateAtom(newAtom: Atom) = {
    Scanamo.exec(dynamo)(Table[Atom](tableName).put(newAtom))
    succeed(newAtom)
  }
}
