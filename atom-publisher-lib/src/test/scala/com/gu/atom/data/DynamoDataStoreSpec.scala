package com.gu.atom.data

import com.gu.contentatom.thrift.Atom
import com.gu.contentatom.thrift.atom.media.MediaAtom
import org.scalatest.{ fixture, Matchers, BeforeAndAfterAll, OptionValues }

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.DynamoFormat._
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import ScanamoUtil._

import com.gu.atom.util.AtomImplicitsGeneral

import cats.syntax.either._

import com.gu.atom.TestData._

class DynamoDataStoreSpec
    extends fixture.FunSpec
    with Matchers
    with OptionValues
    with BeforeAndAfterAll
    with AtomImplicitsGeneral {

  val tableName = "atom-test-table"
  val publishedTableName = "published-atom-test-table"
  val compositeKeyTableName = "composite-key-table"

  case class DataStores(preview: PreviewDynamoDataStore[MediaAtom],
                        published: PublishedDynamoDataStore[MediaAtom],
                        compositeKey: PreviewDynamoDataStore[MediaAtom]
                       )

  type FixtureParam = DataStores

  def withFixture(test: OneArgTest) = {
    val previewDb = new PreviewDynamoDataStore[MediaAtom](LocalDynamoDB.client, tableName) with MediaAtomDynamoFormats
    val compositeKeyDb = new PreviewDynamoDataStore[MediaAtom](LocalDynamoDB.client, compositeKeyTableName) with MediaAtomDynamoFormats
    val publishedDb = new PublishedDynamoDataStore[MediaAtom](LocalDynamoDB.client, publishedTableName) with MediaAtomDynamoFormats
    super.withFixture(test.toNoArgTest(DataStores(previewDb, publishedDb, compositeKeyDb)))
  }

  describe("DynamoDataStore") {
    it("should create a new atom") { dataStores =>
      dataStores.preview.createAtom(testAtom) should equal(Right(testAtom))
    }

    it("should return the atom") { dataStores =>
      dataStores.preview.getAtom(testAtom.id) should equal(Right(testAtom))
    }

    it("should update the atom") { dataStores =>
      val updated = testAtom
        .copy(defaultHtml = "<div>updated</div>")
        .bumpRevision

      dataStores.preview.updateAtom(updated) should equal(Right())
      dataStores.preview.getAtom(testAtom.id) should equal(Right(updated))
    }

    it("should update a published atom") { dataStores =>
      val updated = testAtom
        .copy()
        .withRevision(1)

      dataStores.published.updateAtom(updated) should equal(Right())
      dataStores.published.getAtom(testAtom.id) should equal(Right(updated))
    }

    it("should create the atom with composite key") { dataStores =>
      dataStores.compositeKey.createAtom(DynamoCompositeKey(testAtom.atomType.toString, Some(testAtom.id)), testAtom) should equal(Right(testAtom))
    }

    it("should return the atom with composite key") { dataStores =>
      dataStores.compositeKey.getAtom(DynamoCompositeKey(testAtom.atomType.toString, Some(testAtom.id))) should equal(Right(testAtom))
    }

    it("should update an atom with composite key") { dataStores =>
      val updated = testAtom
        .copy(defaultHtml = "<div>updated</div>")
        .bumpRevision

      dataStores.compositeKey.updateAtom(updated) should equal(Right())
      dataStores.compositeKey.getAtom(DynamoCompositeKey(testAtom.atomType.toString, Some(testAtom.id))) should equal(Right(updated))
    }

    it("should delete an atom if it exists in the table") { dataStores =>
      dataStores.preview.createAtom(testAtomForDeletion) should equal(Right(testAtomForDeletion))
      dataStores.preview.deleteAtom(testAtomForDeletion.id) should equal(Right(testAtomForDeletion))
    }

    it("should delete an atom with composite key if it exists in the table") { dataStores =>
      val key = DynamoCompositeKey(testAtomForDeletion.atomType.toString, Some(testAtomForDeletion.id))
      dataStores.compositeKey.createAtom(key, testAtomForDeletion) should equal(Right(testAtomForDeletion))
      dataStores.compositeKey.deleteAtom(key) should equal(Right(testAtomForDeletion))
    }
  }

  override def beforeAll() = {
    val client = LocalDynamoDB.client
    LocalDynamoDB.createTable(client)(tableName)('id -> S)
    LocalDynamoDB.createTable(client)(publishedTableName)('id -> S)
    LocalDynamoDB.createTable(client)(compositeKeyTableName)('atomType -> S, 'id -> S)
  }
}
