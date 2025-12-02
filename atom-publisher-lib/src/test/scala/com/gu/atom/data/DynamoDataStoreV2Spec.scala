package com.gu.atom.data

import com.gu.atom.TestData._
import com.gu.atom.util.{AtomImplicitsGeneral, JsonSupport}
import com.gu.contentatom.thrift.Atom
import org.scalatest.funspec.FixtureAnyFunSpec
import org.scalatest.matchers.should._
import org.scalatest.{BeforeAndAfterAll, OptionValues}
import software.amazon.awssdk.services.dynamodb.model.{KeyType, ScalarAttributeType}

class DynamoDataStoreV2Spec
    extends FixtureAnyFunSpec
    with Matchers
    with OptionValues
    with BeforeAndAfterAll
    with AtomImplicitsGeneral {

  val tableName = "atom-test-table"
  val publishedTableName = "published-atom-test-table"
  val compositeKeyTableName = "composite-key-table"

  case class DataStoresV2(preview: PreviewDynamoDataStoreV2,
                        published: PublishedDynamoDataStoreV2,
                        compositeKey: PreviewDynamoDataStoreV2
                       )

  type FixtureParam = DataStoresV2

  def withFixture(test: OneArgTest) = {
    val previewDb = new PreviewDynamoDataStoreV2(LocalDynamoDBV2.client(), tableName)
    val compositeKeyDb = new PreviewDynamoDataStoreV2(LocalDynamoDBV2.client(), compositeKeyTableName)
    val publishedDb = new PublishedDynamoDataStoreV2(LocalDynamoDBV2.client(), publishedTableName)
    super.withFixture(test.toNoArgTest(DataStoresV2(previewDb, publishedDb, compositeKeyDb)))
  }

  describe("DynamoDataStore") {
    it("should create a new atom") { dataStores =>
      val atomCreated = dataStores.preview.createAtom(testAtom)
      println(dataStores.preview.listAtoms.map(as => as.map(a => a.id)))
      atomCreated should equal(Right(testAtom))
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
//
//    it("should return the atom with composite key") { dataStores =>
//      dataStores.compositeKey.getAtom(DynamoCompositeKey(testAtom.atomType.toString, Some(testAtom.id))) should equal(Right(testAtom))
//    }
//
//    it("should update an atom with composite key") { dataStores =>
//      val updated = testAtom
//        .copy(defaultHtml = "<div>updated</div>")
//        .bumpRevision
//
//      dataStores.compositeKey.updateAtom(updated) should equal(Right(updated))
//      dataStores.compositeKey.getAtom(DynamoCompositeKey(testAtom.atomType.toString, Some(testAtom.id))) should equal(Right(updated))
//    }
//
//    it("should delete an atom if it exists in the table") { dataStores =>
//      dataStores.preview.createAtom(testAtomForDeletion) should equal(Right(testAtomForDeletion))
//      dataStores.preview.deleteAtom(testAtomForDeletion.id) should equal(Right(testAtomForDeletion))
//    }
//
//    it("should delete an atom with composite key if it exists in the table") { dataStores =>
//      val key = DynamoCompositeKey(testAtomForDeletion.atomType.toString, Some(testAtomForDeletion.id))
//      dataStores.compositeKey.createAtom(key, testAtomForDeletion) should equal(Right(testAtomForDeletion))
//      dataStores.compositeKey.deleteAtom(key) should equal(Right(testAtomForDeletion))
//    }
//
//    it("should decode the old format from dynamo") { dataStores =>
//      val json = dataStores.published.parseJson(
//        """
//          |{
//          |  "defaultHtml" : "<div></div>",
//          |  "data" : {
//          |    "assets" : [
//          |      {
//          |        "id" : "xyzzy",
//          |        "version" : 1,
//          |        "platform" : "Youtube",
//          |        "assetType" : "Video"
//          |      },
//          |      {
//          |        "id" : "fizzbuzz",
//          |        "version" : 2,
//          |        "platform" : "Youtube",
//          |        "assetType" : "Video"
//          |      }
//          |    ],
//          |    "activeVersion" : 2,
//          |    "title" : "Test atom 1",
//          |    "category" : "News"
//          |  },
//          |  "contentChangeDetails" : {
//          |    "revision" : 1
//          |  },
//          |  "id" : "1",
//          |  "atomType" : "Media",
//          |  "labels" : [
//          |  ]
//          |}
//        """.stripMargin).toOption.get
//
//      val atom = json.as[Atom](JsonSupport.backwardsCompatibleAtomDecoder)
//      atom should equal(Right(testAtom))
//    }
  }
  val client = LocalDynamoDBV2.client()
  override def beforeAll() = {
    LocalDynamoDBV2.createTable(client)(tableName)(KeyType.HASH -> "id")
    LocalDynamoDBV2.createTable(client)(publishedTableName)(KeyType.HASH -> "id")
    LocalDynamoDBV2.createTable(client)(compositeKeyTableName)(KeyType.HASH -> "atomType", KeyType.RANGE -> "id")
  }

  override def afterAll(): Unit = {
    LocalDynamoDBV2.deleteTable(client)(tableName)
    LocalDynamoDBV2.deleteTable(client)(publishedTableName)
    LocalDynamoDBV2.deleteTable(client)(compositeKeyTableName)
  }
}
