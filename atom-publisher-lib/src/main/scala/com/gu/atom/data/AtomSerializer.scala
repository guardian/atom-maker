package com.gu.atom.data

import com.gu.contentatom.thrift._
import com.gu.fezziwig.CirceScroogeMacros._
import com.gu.fezziwig.CirceScroogeWhiteboxMacros._
import io.circe._
import io.circe.syntax._

object AtomSerializer {
  import com.gu.atom.util.JsonSupport.encoders._

  def toJson(newAtom: Atom): Json = newAtom.asJson
}