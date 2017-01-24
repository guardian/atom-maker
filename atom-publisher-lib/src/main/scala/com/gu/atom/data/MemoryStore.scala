package data

import cats.syntax.either._
import com.gu.contentatom.thrift.Atom

import com.gu.atom.data._

class MemoryStore extends DataStore {

  def this(initial: Map[DynamoCompositeKey, Atom] = Map.empty) = {
    this()
    dataStore ++= initial
  }

  private val dataStore = collection.mutable.Map[DynamoCompositeKey, Atom]()

  def getAtom(id: String) = dataStore.get(DynamoCompositeKey(id)) match {
    case Some(atom) => succeed(atom)
    case None => fail(IDNotFound)
  }

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[Atom] = dataStore.get(dynamoCompositeKey) match {
    case Some(atom) => succeed(atom)
    case None => fail(IDNotFound)
  }

  def createAtom(atom: Atom) = dataStore.synchronized {
    if(dataStore.get(DynamoCompositeKey(atom.id)).isDefined) {
      fail(IDConflictError)
    } else {
      succeed(dataStore(DynamoCompositeKey(atom.id)) = atom)
    }
  }

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: Atom): DataStoreResult[Unit] = dataStore.synchronized {
    if(dataStore.get(dynamoCompositeKey).isDefined) {
      fail(IDConflictError)
    } else {
      succeed(dataStore(dynamoCompositeKey) = atom)
    }
  }

  def updateAtom(newAtom: Atom) = dataStore.synchronized {
    getAtom(newAtom.id) match {
      case Right(oldAtom) =>
        if(oldAtom.contentChangeDetails.revision >=
             newAtom.contentChangeDetails.revision) {
          fail(VersionConflictError(newAtom.contentChangeDetails.revision))
        } else {
          succeed(dataStore(DynamoCompositeKey(newAtom.id)) = newAtom)
        }
      case Left(_) => fail(IDNotFound)
    }
  }

  def listAtoms = Right(dataStore.values.iterator)
}

class PreviewMemoryStore(initial: Map[DynamoCompositeKey, Atom]) extends MemoryStore(initial) with PreviewDataStore
class PublishedMemoryStore(initial: Map[DynamoCompositeKey, Atom]) extends MemoryStore(initial) with PublishedDataStore
