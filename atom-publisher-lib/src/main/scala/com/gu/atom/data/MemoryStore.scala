package data

import com.gu.atom.data.DataStore.AtomSkeleton
import com.gu.atom.data._
import com.gu.contentatom.thrift.Atom
import com.gu.draftcontentatom.thrift.{Atom => Draft}

abstract class MemoryStore extends DataStore {

  val atomSkeleton: AtomSkeleton[DataType]

  def addAll(items: Map[DynamoCompositeKey, DataType]) = dataStore ++= items

  private val dataStore = collection.mutable.Map[DynamoCompositeKey, DataType]()

  def getAtom(id: String): DataStoreResult[DataType] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[DataType] = dataStore.get(dynamoCompositeKey) match {
    case Some(atom) => succeed(atom)
    case None => fail(IDNotFound)
  }

  def createAtom(atom: DataType): DataStoreResult[DataType] = {
    val id = atomSkeleton.getId(atom)
    createAtom(DynamoCompositeKey(id), atom)
  }

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: DataType): DataStoreResult[DataType] = dataStore.synchronized {
    if(dataStore.get(dynamoCompositeKey).isDefined) {
      fail(IDConflictError)
    } else {
      dataStore(dynamoCompositeKey) = atom
      succeed(atom)
    }
  }

  def updateAtom(newAtom: DataType) = dataStore.synchronized {
    val newAtomId = atomSkeleton.getId(newAtom)
    val newAtomRevision = atomSkeleton.getContentChangeDetails(newAtom).revision

    getAtom(newAtomId) match {
      case Right(oldAtom) =>
        val oldAtomRevision = atomSkeleton.getContentChangeDetails(oldAtom).revision
        if(oldAtomRevision >=
             newAtomRevision) {
          fail(VersionConflictError(newAtomRevision))
        } else {
          dataStore(DynamoCompositeKey(newAtomId)) = newAtom
          succeed(newAtom)
        }
      case Left(_) => fail(IDNotFound)
    }
  }

  def listAtoms = Right(dataStore.values.iterator)

  def deleteAtom(id: String): DataStoreResult[DataType] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[DataType] =
    dataStore.remove(dynamoCompositeKey) match {
      case Some(atom) => succeed(atom)
      case _ => fail(IDNotFound)
    }
}

class PreviewMemoryStore()(implicit override val atomSkeleton: AtomSkeleton[Atom]) extends MemoryStore() with PreviewDataStore
class PublishedMemoryStore()(implicit override val atomSkeleton: AtomSkeleton[Atom]) extends MemoryStore() with PublishedDataStore
class DraftDynamoMemoryStore()(implicit override val atomSkeleton: AtomSkeleton[Draft]) extends MemoryStore() with DraftDataStore