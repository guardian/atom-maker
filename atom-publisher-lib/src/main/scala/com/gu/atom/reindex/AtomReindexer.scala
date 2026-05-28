package com.gu.atom.reindex

import scala.util.Try

trait AtomReindexer {

  val reindexDataStore: ReindexDataStore

  def startReindex(): Try[ReindexJob]

  def getReindexStatus(): Option[ReindexJob]

  def cancelReindex(): Option[ReindexJob]

}

trait PreviewAtomReindexer extends AtomReindexer
trait PublishedAtomReindexer extends AtomReindexer
