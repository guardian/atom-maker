package com.gu.atom.data

import org.scalatest.{ Matchers, FunSpec }

import cats.data.Xor

import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media._
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.scrooge.ScroogeDynamoFormat
//import com.gu.atom.data.AtomDynamoFormats._

import AtomData._
import ScroogeDynamoFormat._
import DynamoFormat._
import ScanamoUtil._

class AtomDynamoFormatsSpec extends FunSpec with Matchers {
  //implicit val shortFmt = DynamoFormat.xmap[Short, Int](i => Xor.Right(i.toShort))(_.toInt)

  val testAtomData: AtomData = AtomData.Media(MediaAtom(
    assets = Nil,
    activeVersion = Some(1L),
    title = "Test media atom",
    category = Category.Feature,
    plutoProjectId = Some("PlutoId"),
    duration = None,
    source = Some("YouTube"),
    posterUrl = None,
    description = Some("Description"),
    metadata = Some(Metadata(
      tags = Some(Seq("tag1", "tag2")),
      categoryId = Some("categoryId"),
      license = None,
      commentsEnabled = Some(true),
      channelId = None,
      privacyStatus = Some(PrivacyStatus.Private)
    ))))

  describe("atomdata dynamo format") {
    it("should convert test atom") {
      import MediaAtomDynamoFormats._

      (DynamoFormat[AtomData].read(DynamoFormat[AtomData].write(testAtomData))
         should equal(Xor.right(testAtomData)))
    }
  }
}
