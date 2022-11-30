package com.gu.atom.util

import cats.syntax.either._
import com.gu.contentatom.thrift._
import com.gu.contententity.thrift.Entity
import com.gu.fezziwig.CirceScroogeMacros._
import com.twitter.scrooge.ThriftEnum
import io.circe._

object JsonSupport {

  /**
    * For backwards-compatibility, encode enums as e.g. 'Profile'.
    * Fezziwig encodes using the same formatting as the original thrift definition.
    */
  implicit def thriftEnumEncoder[T <: ThriftEnum]: Encoder[T] = Encoder[String].contramap(_.name)

  //Get a real Atom decoder from fezziwig
  private def realAtomDecoder[ATOM <: Atom](implicit decoder: Decoder[ATOM]) = {
    //These implicits speed up compilation
    implicit val entityDecoder = Decoder[Entity]
    implicit val imageAssetDecoder = Decoder[ImageAsset]
    implicit val imageDecoder = Decoder[Image]
    implicit val changeRecord = Decoder[ChangeRecord]
    implicit val atomDataDecoder = Decoder[AtomData]
    implicit val flagsDecoder = Decoder[Flags]

    Decoder[ATOM]
  }

  /**
    * A custom decoder for backwards-compatibility.
    *
    * The old format in dynamo did not include the type under the `data` union field. E.g.
    * Old format:
    * ```
    * {
    *   data: {
    *     items: []
    *   ]
    * }
    * ```
    *
    * New format:
    * ```
    * {
    *   data: {
    *     profile: {
    *       items: []
    *     }
    *   }
    * }
    * ```
    *
    * This decoder supports either format.
    */
  def backwardsCompatibleAtomDecoder[ATOM <: Atom](implicit decoder: Decoder[ATOM]) = new Decoder[ATOM] {
    final def apply(c: HCursor): Decoder.Result[ATOM] = {
      val result: Option[Decoder.Result[ATOM]] = for {
        topLevelObj <- c.value.asObject
        atomTypeJson <- topLevelObj("atomType")
        atomType <- atomTypeJson.asString
        dataJson <- topLevelObj("data")
        data <- dataJson.asObject
      } yield {
        val fixedJson = {
          val nestedDataFieldName = atomType.toLowerCase

          //Special case for commonsDivision because the scrooge enum value loses the casing
          if (data(nestedDataFieldName).nonEmpty || nestedDataFieldName == "commonsdivision") c.value
          else {
            //Add the union type name under `data`
            val newData = JsonObject.fromIterable(Seq(nestedDataFieldName -> dataJson))
            Json.fromJsonObject(topLevelObj.add("data", Json.fromJsonObject(newData)))
          }
        }

        fixedJson.as[ATOM](realAtomDecoder)
      }

      result.getOrElse(Left(DecodingFailure("Json does not match Atom format", c.history)))
    }
  }
}
