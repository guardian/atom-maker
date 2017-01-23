package data

import cats.syntax.either._
import com.gu.contentatom.thrift.Atom

import com.gu.atom.data._

class MemoryStore extends DataStore {

  def this(initial: Map[String, Atom] = Map.empty) = {
    this()
    dataStore ++= initial
  }

  private val dataStore = collection.mutable.Map[String, Atom]()

  def getAtom(id: String) = dataStore.get(id) match {
    case Some(atom) => succeed(atom)
    case None => fail(IDNotFound)
  }

  def createAtom(atom: Atom) = dataStore.synchronized {
    if(dataStore.get(atom.id).isDefined) {
      fail(IDConflictError)
    } else {
      succeed(dataStore(atom.id) = atom)
    }
  }

  def updateAtom(newAtom: Atom) = dataStore.synchronized {
    getAtom(newAtom.id) match {
      case Right(oldAtom) =>
        if(oldAtom.contentChangeDetails.revision >=
             newAtom.contentChangeDetails.revision) {
          fail(VersionConflictError(newAtom.contentChangeDetails.revision))
        } else {
          succeed(dataStore(newAtom.id) = newAtom)
        }
      case Left(_) => fail(IDNotFound)
    }
  }

  def listAtoms = Right(dataStore.values.iterator)
}

class PreviewMemoryStore(initial: Map[String, Atom]) extends MemoryStore(initial) with PreviewDataStore
class PublishedMemoryStore(initial: Map[String, Atom]) extends MemoryStore(initial) with PublishedDataStore
