package com.gu.atom.data

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType._
import com.gu.atom.TestData._
import com.gu.atom.util.AtomImplicitsGeneral
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, fixture}
import com.gu.draftcontentatom.thrift.{Atom => Draft}

import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.gu.atom.data.AtomDynamoFormats._

class DynamoDataStoreSpec
    extends fixture.FunSpec
    with Matchers
    with OptionValues
    with BeforeAndAfterAll
    with AtomImplicitsGeneral {

  val tableName = "atom-test-table"
  val publishedTableName = "published-atom-test-table"
  val compositeKeyTableName = "composite-key-table"
  val draftTableName = "draft-atom-test-table"

  case class DataStores(preview: PreviewDynamoDataStore,
                        published: PublishedDynamoDataStore,
                        compositeKey: PreviewDynamoDataStore,
                        draft: DraftDynamoDataStore
                       )

  type FixtureParam = DataStores

  def withFixture(test: OneArgTest) = {
    val previewDb = new PreviewDynamoDataStore(LocalDynamoDB.client, tableName)
    val compositeKeyDb = new PreviewDynamoDataStore(LocalDynamoDB.client, compositeKeyTableName)
    val publishedDb = new PublishedDynamoDataStore(LocalDynamoDB.client, publishedTableName)
    val draftDb = new DraftDynamoDataStore(LocalDynamoDB.client, draftTableName)
    super.withFixture(test.toNoArgTest(DataStores(previewDb, publishedDb, compositeKeyDb, draftDb)))
  }

  describe("DynamoDataStore") {
    it("should create a new atom") { dataStores =>
      dataStores.preview.createAtom(testAtom) should equal(Right(testAtom))
    }

    it("should list all atoms of all types") { dataStores =>
      dataStores.preview.createAtom(testAtoms(1))
      dataStores.preview.createAtom(testAtoms(2))
      dataStores.preview.listAtoms.map(_.toList).fold(identity, res => res should contain theSameElementsAs testAtoms)
    }

    it("should return the atom") { dataStores =>
      dataStores.preview.getAtom(testAtom.id) should equal(Right(testAtom))
    }

    it("should update the atom") { dataStores =>
      val updated = testAtom
        .copy(defaultHtml = "<div>updated</div>")
        .bumpRevision

      dataStores.preview.updateAtom(updated) should equal(Right(updated))
      dataStores.preview.getAtom(testAtom.id) should equal(Right(updated))
    }

    it("should update a published atom") { dataStores =>
      val updated = testAtom
        .copy()
        .withRevision(1)

      dataStores.published.updateAtom(updated) should equal(Right(updated))
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

      dataStores.compositeKey.updateAtom(updated) should equal(Right(updated))
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

    it("should create a new draft atom") { dataStores =>
      dataStores.draft.createAtom(testDraftAtom) should equal(Right(testDraftAtom))
    }

    it("should list all draft atoms") { dataStores =>
      dataStores.draft.createAtom(testDraftAtoms(1))
      dataStores.draft.createAtom(testDraftAtoms(2))
      dataStores.draft.listAtoms.map(_.toList).fold(identity, res => res should contain theSameElementsAs testDraftAtoms)
    }

    it("should return a specific draft atom") { dataStores =>
      dataStores.draft.getAtom(testDraftAtom.id.get) should equal(Right(testDraftAtom))
    }

    it("should update the draft atom") { dataStores =>
      val updated: Draft = testDraftAtom.copy(defaultHtml = Some("<div>updated</div>"))

      dataStores.draft.updateAtom(updated) should equal(Right(updated))
      dataStores.draft.getAtom(testDraftAtom.id.get) should equal(Right(updated))
    }
  }

  override def beforeAll() = {
    val client = LocalDynamoDB.client
    LocalDynamoDB.createTable(client)(tableName)('id -> S)
    LocalDynamoDB.createTable(client)(publishedTableName)('id -> S)
    LocalDynamoDB.createTable(client)(compositeKeyTableName)('atomType -> S, 'id -> S)
    LocalDynamoDB.createTable(client)(draftTableName)('id -> S)
  }
}
