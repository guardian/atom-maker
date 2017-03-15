package data

import com.gu.atom.data.DataStore.AtomSkeleton
import com.gu.atom.data.DataStore._
import com.gu.atom.data._

class MemoryStore[A]()(implicit val atomSkeleton: AtomSkeleton[A]) extends DataStore[A] {

  def this(initial: Map[DynamoCompositeKey, A] = Map.empty)(implicit atomSkeleton: AtomSkeleton[A]) = {
    this()
    dataStore ++= initial
  }

  private val dataStore = collection.mutable.Map[DynamoCompositeKey, A]()

  def getAtom(id: String): DataStoreResult[A] = getAtom(DynamoCompositeKey(id))

  def getAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[A] = dataStore.get(dynamoCompositeKey) match {
    case Some(atom) => succeed(atom)
    case None => fail(IDNotFound)
  }

  def createAtom(atom: A): DataStoreResult[A] = {
    val id = atomSkeleton.getId(atom)
    createAtom(DynamoCompositeKey(id), atom)
  }

  def createAtom(dynamoCompositeKey: DynamoCompositeKey, atom: A): DataStoreResult[A] = dataStore.synchronized {
    if(dataStore.get(dynamoCompositeKey).isDefined) {
      fail(IDConflictError)
    } else {
      dataStore(dynamoCompositeKey) = atom
      succeed(atom)
    }
  }

  def updateAtom(newAtom: A) = dataStore.synchronized {
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

  def deleteAtom(id: String): DataStoreResult[A] = deleteAtom(DynamoCompositeKey(id))

  def deleteAtom(dynamoCompositeKey: DynamoCompositeKey): DataStoreResult[A] =
    dataStore.remove(dynamoCompositeKey) match {
      case Some(atom) => succeed(atom)
      case _ => fail(IDNotFound)
    }
}

class PreviewMemoryStore[A](initial: Map[DynamoCompositeKey, A])(implicit override val atomSkeleton: AtomSkeleton[A]) extends MemoryStore(initial) with PreviewDataStore[A]
class PublishedMemoryStore[A](initial: Map[DynamoCompositeKey, A])(implicit override val atomSkeleton: AtomSkeleton[A]) extends MemoryStore(initial) with PublishedDataStore[A]
