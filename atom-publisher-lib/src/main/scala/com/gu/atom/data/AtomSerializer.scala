package com.gu.atom.data

import com.gu.contentatom.thrift.Atom
import io.circe.Json
import io.circe.syntax._
import com.gu.fezziwig.CirceScroogeMacros.encodeThriftStruct
object AtomSerializer {

  def toJson(newAtom: Atom): Json = newAtom.asJson
}