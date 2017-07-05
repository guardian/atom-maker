package com.gu.atom.util

import com.gu.contentatom.thrift._
import io.circe._
import com.gu.fezziwig.CirceScroogeMacros._
import com.gu.contententity.thrift.Entity
import cats.syntax.either._
import com.twitter.scrooge.ThriftEnum

object JsonSupport {

  /**
    * For backwards-compatibility, encode enums as e.g. 'Profile'.
    * Fezziwig encodes using the same formatting as the original thrift definition.
    */
  implicit def thriftEnumEncoder[T <: ThriftEnum]: Encoder[T] = Encoder[String].contramap(_.name)

  //Get a real Atom decoder from fezziwig
  private val realAtomDecoder = {
    //These implicits speed up compilation
    implicit val entityDecoder = Decoder[Entity]
    implicit val imageAssetDecoder = Decoder[ImageAsset]
    implicit val imageDecoder = Decoder[Image]
    implicit val changeRecord = Decoder[ChangeRecord]
    implicit val atomDataDecoder = Decoder[AtomData]
    implicit val flagsDecoder = Decoder[Flags]

    Decoder[Atom]
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
  val backwardsCompatibleAtomDecoder: Decoder[Atom] = new Decoder[Atom] {
    final def apply(c: HCursor): Decoder.Result[Atom] = {

      val result: Option[Decoder.Result[Atom]] = for {
        topLevelObj <- c.value.asObject
        atomTypeJson <- topLevelObj("atomType")
        atomType <- atomTypeJson.asString
        dataJson <- topLevelObj("data")
        data <- dataJson.asObject
      } yield {
        val fixedJson = {
          val nestedDataFieldName = atomType.toLowerCase

          if (data(nestedDataFieldName).nonEmpty) c.value
          else {
            //Add the union type name under `data`
            val newData = JsonObject.fromIterable(Seq(nestedDataFieldName -> dataJson))
            Json.fromJsonObject(topLevelObj.add("data", Json.fromJsonObject(newData)))
          }
        }

        fixedJson.as[Atom](realAtomDecoder)
      }

      result.getOrElse(Left(DecodingFailure("Json does not match Atom format", c.history)))
    }
  }
}
