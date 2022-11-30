package com.gu.atom.data

import com.gu.contentatom.thrift.Atom

object AtomGenericsTest {
  val fixture = new PreviewDynamoDataStore[Atom]()
}
