package com.gu.atom.reindex

trait ReindexDataStore {
  def create(documentsExpected: Long): Option[ReindexJob]

  def get(): Option[ReindexJob]

  def getInProgress(): Option[ReindexJob]

  def recordProgress(documentsIndexed: Long): Unit

  def markComplete(reindexJob: ReindexJob): ReindexJob

  def markCancelled(reindexJob: ReindexJob): ReindexJob

  def markFailed(reindexJob: ReindexJob): ReindexJob
}
