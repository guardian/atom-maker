package data

import com.gu.atom.data._
import com.gu.contentatom.thrift.Atom

class MemoryStore extends DataStore {

  def this(initial: Map[DynamoCompositeKey, Atom] = Map.empty) = {
    this()
    dataStore ++= initial
  }

  private val dataStore = collection.mutable.Map[DynamoCompositeKey, Atom]()

  def getAtom(id: String): DataStoreResult[Atom] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] = dataStore.get(dynamoCompositeKey) match {
    case Some(atom) => succeed(atom)
    case None => fail(IDNotFound)
  }

  def createAtom(atom: Atom): DataStoreResult[Atom] = createAtom(DynamoCompositeKey(atom.id), atom)

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Atom] = dataStore.synchronized {
    if(dataStore.get(dynamoCompositeKey).isDefined) {
      fail(IDConflictError)
    } else {
      dataStore(dynamoCompositeKey) = atom
      succeed(atom)
    }
  }

  def updateAtom(newAtom: Atom) = dataStore.synchronized {
    getAtom(newAtom.id) match {
      case Right(oldAtom) =>
        if(oldAtom.contentChangeDetails.revision >=
             newAtom.contentChangeDetails.revision) {
          fail(VersionConflictError(newAtom.contentChangeDetails.revision))
        } else {
          dataStore(DynamoCompositeKey(newAtom.id)) = newAtom
          succeed(newAtom)
        }
      case Left(_) => fail(IDNotFound)
    }
  }

  def listAtoms = Right(dataStore.values.iterator)

  def deleteAtom(id: String): DataStoreResult[Atom] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] =
    dataStore.remove(dynamoCompositeKey) match {
      case Some(atom) => succeed(atom)
      case _ => fail(IDNotFound)
    }
}

class PreviewMemoryStore(initial: Map[DynamoCompositeKey, Atom]) extends MemoryStore(initial) with PreviewDataStore
class PublishedMemoryStore(initial: Map[DynamoCompositeKey, Atom]) extends MemoryStore(initial) with PublishedDataStore
