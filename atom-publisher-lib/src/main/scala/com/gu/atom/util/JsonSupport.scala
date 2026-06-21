package com.gu.atom.util

import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.audio._
import com.gu.contentatom.thrift.atom.chart._
import com.gu.contentatom.thrift.atom.commonsdivision._
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.emailsignup.EmailSignUpAtom
import com.gu.contentatom.thrift.atom.explainer._
import com.gu.contentatom.thrift.atom.guide._
import com.gu.contentatom.thrift.atom.interactive.InteractiveAtom
import com.gu.contentatom.thrift.atom.media.{Asset => _, _}
import com.gu.contentatom.thrift.atom.profile._
import com.gu.contentatom.thrift.atom.qanda._
import com.gu.contentatom.thrift.atom.{media, quiz}
import com.gu.contentatom.thrift.atom.quiz.{Asset => _, _}
import com.gu.contentatom.thrift.atom.review._
import com.gu.contentatom.thrift.atom.timeline._
import com.gu.contententity.thrift._
import com.gu.contententity.thrift.entity.film.Film
import com.gu.contententity.thrift.entity.game.Game
import com.gu.contententity.thrift.entity.organisation.Organisation
import com.gu.contententity.thrift.entity.person.Person
import com.gu.contententity.thrift.entity.place.Place
import com.gu.contententity.thrift.entity.restaurant.Restaurant
import com.twitter.scrooge.ThriftEnum
import io.circe._
import io.circe.generic.semiauto._

object JsonSupport {

  object encoders {

    import com.gu.fezziwig.CirceScroogeMacros._
    import com.gu.fezziwig.CirceScroogeWhiteboxMacros._

    implicit def thriftEnumEncoder[T <: ThriftEnum]: Encoder[T] = Encoder[String].contramap(_.name)

    // audio.thrift
    implicit lazy val audioAtomEncoder: Encoder[AudioAtom] = deriveEncoder
    implicit lazy val offPlatformEncoder: Encoder[OffPlatform] = deriveEncoder

    // chart.thrift
    implicit lazy val axisEncoder: Encoder[Axis] = deriveEncoder
    implicit lazy val chartAtomEncoder: Encoder[ChartAtom] = deriveEncoder
    implicit lazy val chartTypeEncoder: Encoder[ChartType] = deriveEncoder
    implicit lazy val displaySettingsEncoder: Encoder[DisplaySettings] = deriveEncoder
    implicit lazy val furnitureEncoder: Encoder[Furniture] = deriveEncoder
    implicit lazy val rangeEncoder: Encoder[Range] = deriveEncoder
    implicit lazy val rowTypeEncoder: Encoder[RowType] = deriveEncoder
    implicit lazy val seriesColourEncoder: Encoder[SeriesColour] = deriveEncoder
    implicit lazy val tabularDataEncoder: Encoder[TabularData] = deriveEncoder

    // commonsdivision.thrift
    implicit lazy val commonsDivisionEncoder: Encoder[CommonsDivision] = deriveEncoder
    implicit lazy val mpEncoder: Encoder[Mp] = deriveEncoder
    implicit lazy val votesEncoder: Encoder[Votes] = deriveEncoder

    // cta.thrift
    implicit lazy val ctaAtomEncoder: Encoder[CTAAtom] = deriveEncoder

    // emailsignup.thrift
    implicit lazy val emailSignUpAtomEncoder: Encoder[EmailSignUpAtom] = deriveEncoder

    // explainer.thrift
    implicit lazy val displayTypeEncoder: Encoder[DisplayType] = deriveEncoder
    implicit lazy val explainerAtomEncoder: Encoder[ExplainerAtom] = deriveEncoder

    // guide.thrift
    implicit lazy val guideAtomEncoder: Encoder[GuideAtom] = deriveEncoder
    implicit lazy val guideItemEncoder: Encoder[GuideItem] = deriveEncoder

    // interactive.thrift
    implicit lazy val interactiveEncoder: Encoder[InteractiveAtom] = deriveEncoder

    // media.thrift
    implicit lazy val assetEncoder: Encoder[media.Asset] = deriveEncoder
    implicit lazy val assetTypeEncoder: Encoder[AssetType] = deriveEncoder
    implicit lazy val categoryEncoder: Encoder[Category] = deriveEncoder
    implicit lazy val iconikDataEncoder: Encoder[IconikData] = deriveEncoder
    implicit lazy val mediaAtomEncoder: Encoder[MediaAtom] = deriveEncoder
    implicit lazy val metadataEncoder: Encoder[Metadata] = deriveEncoder
    implicit lazy val platformEncoder: Encoder[Platform] = deriveEncoder
    implicit lazy val plutoDataEncoder: Encoder[PlutoData] = deriveEncoder
    implicit lazy val youtubeDataEncoder: Encoder[YoutubeData] = deriveEncoder
    implicit lazy val selfHostDataEncoder: Encoder[SelfHostData] = deriveEncoder
    implicit lazy val privacyStatusEncoder: Encoder[PrivacyStatus] = deriveEncoder
    implicit lazy val videoPlayerFormatEncoder: Encoder[VideoPlayerFormat] = deriveEncoder

    // quiz.thrift
    implicit lazy val quizAssetEncoder: Encoder[quiz.Asset] = deriveEncoder
    implicit lazy val answerEncoder: Encoder[Answer] = deriveEncoder
    implicit lazy val questionEncoder: Encoder[Question] = deriveEncoder
    implicit lazy val resultGroupEncoder: Encoder[ResultGroup] = deriveEncoder
    implicit lazy val resultGroupsEncoder: Encoder[ResultGroups] = deriveEncoder
    implicit lazy val resultBucketEncoder: Encoder[ResultBucket] = deriveEncoder
    implicit lazy val resultBucketsEncoder: Encoder[ResultBuckets] = deriveEncoder
    implicit lazy val quizContentEncoder: Encoder[QuizContent] = deriveEncoder
    implicit lazy val quizAtomEncoder: Encoder[QuizAtom] = deriveEncoder

    // profile.thrift
    implicit lazy val profileItemEncoder: Encoder[ProfileItem] = deriveEncoder
    implicit lazy val profileAtomEncoder: Encoder[ProfileAtom] = deriveEncoder

    // qanda.thrift
    implicit lazy val qAndAItemEncoder: Encoder[QAndAItem] = deriveEncoder
    implicit lazy val qAndAAtomEncoder: Encoder[QAndAAtom] = deriveEncoder

    // review.thrift
    implicit lazy val reviewTypeEncoder: Encoder[ReviewType] = deriveEncoder
    implicit lazy val ratingEncoder: Encoder[Rating] = deriveEncoder
    implicit lazy val reviewAtomEncoder: Encoder[ReviewAtom] = deriveEncoder

    // timeline.thrift
    implicit lazy val timelineItemEncoder: Encoder[TimelineItem] = deriveEncoder
    implicit lazy val timelineAtomEncoder: Encoder[TimelineAtom] = deriveEncoder

    // entity/shared.thrift (com.gu.contententity.thrift)
    implicit lazy val priceEncoder: Encoder[Price] = deriveEncoder
    implicit lazy val geolocationEncoder: Encoder[Geolocation] = deriveEncoder
    implicit lazy val addressEncoder: Encoder[Address] = deriveEncoder

    // entity/person.thrift (com.gu.contententity.thrift.entity.person)
    implicit lazy val personEncoder: Encoder[Person] = deriveEncoder

    // entity/film.thrift (com.gu.contententity.thrift.entity.film)
    implicit lazy val filmEncoder: Encoder[Film] = deriveEncoder

    // entity/game.thrift (com.gu.contententity.thrift.entity.game)
    implicit lazy val gameEncoder: Encoder[Game] = deriveEncoder

    // entity/restaurant.thrift (com.gu.contententity.thrift.entity.restaurant)
    implicit lazy val restaurantEncoder: Encoder[Restaurant] = deriveEncoder

    // entity/place.thrift (com.gu.contententity.thrift.entity.place)
    implicit lazy val placeEncoder: Encoder[Place] = deriveEncoder

    // entity/organisation.thrift (com.gu.contententity.thrift.entity.organisation)
    implicit lazy val organisationEncoder: Encoder[Organisation] = deriveEncoder

    // entity/entity.thrift (com.gu.contententity.thrift)
    implicit lazy val entityTypeEncoder: Encoder[EntityType] = deriveEncoder
    implicit lazy val entityEncoder: Encoder[Entity] = deriveEncoder

    // contentatom.thrift
    implicit lazy val atomDataEncoder: Encoder[AtomData] = deriveEncoder
    implicit lazy val atomTypeEncoder: Encoder[AtomType] = deriveEncoder
    implicit lazy val changeRecordEncoder: Encoder[ChangeRecord] = deriveEncoder
    implicit lazy val contentAtomEventEncoder: Encoder[ContentAtomEvent] = deriveEncoder
    implicit lazy val contentChangeDetailsEncoder: Encoder[ContentChangeDetails] = deriveEncoder
    implicit lazy val emailProviderEncoder: Encoder[EmailProvider] = deriveEncoder
    implicit lazy val eventTypeEncoder: Encoder[EventType] = deriveEncoder
    implicit lazy val flagsEncoder: Encoder[Flags] = deriveEncoder
    implicit lazy val imageEncoder: Encoder[Image] = deriveEncoder
    implicit lazy val imageAssetEncoder: Encoder[ImageAsset] = deriveEncoder
    implicit lazy val imageAssetDimensionsEncoder: Encoder[ImageAssetDimensions] = deriveEncoder
    implicit lazy val newspaperEncoder: Encoder[Newspaper] = deriveEncoder
    implicit lazy val notificationProvidersEncoder: Encoder[NotificationProviders] = deriveEncoder
    implicit lazy val referenceEncoder: Encoder[Reference] = deriveEncoder
    implicit lazy val sectionEncoder: Encoder[Section] = deriveEncoder
    implicit lazy val tagEncoder: Encoder[Tag] = deriveEncoder
    implicit lazy val tagUsageEncoder: Encoder[TagUsage] = deriveEncoder
    implicit lazy val taxonomyEncoder: Encoder[Taxonomy] = deriveEncoder
    implicit lazy val userEncoder: Encoder[User] = deriveEncoder

    implicit lazy val atomEncoder: Encoder[Atom] = deriveEncoder
  }

  object decoders {

    import com.gu.fezziwig.CirceScroogeMacros._
    import com.gu.fezziwig.CirceScroogeWhiteboxMacros._

    // audio.thrift
    implicit lazy val audioAtomDecoder: Decoder[AudioAtom] = deriveDecoder
    implicit lazy val offPlatformDecoder: Decoder[OffPlatform] = deriveDecoder

    // chart.thrift
    implicit lazy val axisDecoder: Decoder[Axis] = deriveDecoder
    implicit lazy val chartAtomDecoder: Decoder[ChartAtom] = deriveDecoder
    implicit lazy val chartTypeDecoder: Decoder[ChartType] = deriveDecoder
    implicit lazy val displaySettingsDecoder: Decoder[DisplaySettings] = deriveDecoder
    implicit lazy val furnitureDecoder: Decoder[Furniture] = deriveDecoder
    implicit lazy val rangeDecoder: Decoder[Range] = deriveDecoder
    implicit lazy val rowTypeDecoder: Decoder[RowType] = deriveDecoder
    implicit lazy val seriesColourDecoder: Decoder[SeriesColour] = deriveDecoder
    implicit lazy val tabularDataDecoder: Decoder[TabularData] = deriveDecoder

    // commonsdivision.thrift
    implicit lazy val commonsDivisionDecoder: Decoder[CommonsDivision] = deriveDecoder
    implicit lazy val mpDecoder: Decoder[Mp] = deriveDecoder
    implicit lazy val votesDecoder: Decoder[Votes] = deriveDecoder

    // cta.thrift
    implicit lazy val ctaAtomDecoder: Decoder[CTAAtom] = deriveDecoder

    // emailsignup.thrift
    implicit lazy val emailSignUpAtomDecoder: Decoder[EmailSignUpAtom] = deriveDecoder

    // explainer.thrift
    implicit lazy val displayTypeDecoder: Decoder[DisplayType] = deriveDecoder
    implicit lazy val explainerAtomDecoder: Decoder[ExplainerAtom] = deriveDecoder

    // guide.thrift
    implicit lazy val guideAtomDecoder: Decoder[GuideAtom] = deriveDecoder
    implicit lazy val guideItemDecoder: Decoder[GuideItem] = deriveDecoder

    // interactive.thrift
    implicit lazy val interactiveDecoder: Decoder[InteractiveAtom] = deriveDecoder

    // media.thrift
    implicit lazy val assetDecoder: Decoder[media.Asset] = deriveDecoder
    implicit lazy val assetTypeDecoder: Decoder[AssetType] = deriveDecoder
    implicit lazy val categoryDecoder: Decoder[Category] = deriveDecoder
    implicit lazy val iconikDataDecoder: Decoder[IconikData] = deriveDecoder
    implicit lazy val mediaAtomDecoder: Decoder[MediaAtom] = deriveDecoder
    implicit lazy val metadataDecoder: Decoder[Metadata] = deriveDecoder
    implicit lazy val platformDecoder: Decoder[Platform] = deriveDecoder
    implicit lazy val plutoDataDecoder: Decoder[PlutoData] = deriveDecoder
    implicit lazy val youtubeDataDecoder: Decoder[YoutubeData] = deriveDecoder
    implicit lazy val selfHostDataDecoder: Decoder[SelfHostData] = deriveDecoder
    implicit lazy val privacyStatusDecoder: Decoder[PrivacyStatus] = deriveDecoder
    implicit lazy val videoPlayerFormatDecoder: Decoder[VideoPlayerFormat] = deriveDecoder

    // quiz.thrift
    implicit lazy val quizAssetDecoder: Decoder[quiz.Asset] = deriveDecoder
    implicit lazy val answerDecoder: Decoder[Answer] = deriveDecoder
    implicit lazy val questionDecoder: Decoder[Question] = deriveDecoder
    implicit lazy val resultGroupDecoder: Decoder[ResultGroup] = deriveDecoder
    implicit lazy val resultGroupsDecoder: Decoder[ResultGroups] = deriveDecoder
    implicit lazy val resultBucketDecoder: Decoder[ResultBucket] = deriveDecoder
    implicit lazy val resultBucketsDecoder: Decoder[ResultBuckets] = deriveDecoder
    implicit lazy val quizContentDecoder: Decoder[QuizContent] = deriveDecoder
    implicit lazy val quizAtomDecoder: Decoder[QuizAtom] = deriveDecoder

    // profile.thrift
    implicit lazy val profileItemDecoder: Decoder[ProfileItem] = deriveDecoder
    implicit lazy val profileAtomDecoder: Decoder[ProfileAtom] = deriveDecoder

    // qanda.thrift
    implicit lazy val qAndAItemDecoder: Decoder[QAndAItem] = deriveDecoder
    implicit lazy val qAndAAtomDecoder: Decoder[QAndAAtom] = deriveDecoder

    // review.thrift
    implicit lazy val reviewTypeDecoder: Decoder[ReviewType] = deriveDecoder
    implicit lazy val ratingDecoder: Decoder[Rating] = deriveDecoder
    implicit lazy val reviewAtomDecoder: Decoder[ReviewAtom] = deriveDecoder

    // timeline.thrift
    implicit lazy val timelineItemDecoder: Decoder[TimelineItem] = deriveDecoder
    implicit lazy val timelineAtomDecoder: Decoder[TimelineAtom] = deriveDecoder

    // entity/shared.thrift (com.gu.contententity.thrift)
    implicit lazy val priceDecoder: Decoder[Price] = deriveDecoder
    implicit lazy val geolocationDecoder: Decoder[Geolocation] = deriveDecoder
    implicit lazy val addressDecoder: Decoder[Address] = deriveDecoder

    // entity/person.thrift (com.gu.contententity.thrift.entity.person)
    implicit lazy val personDecoder: Decoder[Person] = deriveDecoder

    // entity/film.thrift (com.gu.contententity.thrift.entity.film)
    implicit lazy val filmDecoder: Decoder[Film] = deriveDecoder

    // entity/game.thrift (com.gu.contententity.thrift.entity.game)
    implicit lazy val gameDecoder: Decoder[Game] = deriveDecoder

    // entity/restaurant.thrift (com.gu.contententity.thrift.entity.restaurant)
    implicit lazy val restaurantDecoder: Decoder[Restaurant] = deriveDecoder

    // entity/place.thrift (com.gu.contententity.thrift.entity.place)
    implicit lazy val placeDecoder: Decoder[Place] = deriveDecoder

    // entity/organisation.thrift (com.gu.contententity.thrift.entity.organisation)
    implicit lazy val organisationDecoder: Decoder[Organisation] = deriveDecoder

    // entity/entity.thrift (com.gu.contententity.thrift)
    implicit lazy val entityTypeDecoder: Decoder[EntityType] = deriveDecoder
    implicit lazy val entityDecoder: Decoder[Entity] = deriveDecoder

    // contentatom.thrift
    implicit lazy val atomDataDecoder: Decoder[AtomData] = deriveDecoder
    implicit lazy val atomTypeDecoder: Decoder[AtomType] = deriveDecoder
    implicit lazy val changeRecordDecoder: Decoder[ChangeRecord] = deriveDecoder
    implicit lazy val contentAtomEventDecoder: Decoder[ContentAtomEvent] = deriveDecoder
    implicit lazy val contentChangeDetailsDecoder: Decoder[ContentChangeDetails] = deriveDecoder
    implicit lazy val emailProviderDecoder: Decoder[EmailProvider] = deriveDecoder
    implicit lazy val eventTypeDecoder: Decoder[EventType] = deriveDecoder
    implicit lazy val flagsDecoder: Decoder[Flags] = deriveDecoder
    implicit lazy val imageDecoder: Decoder[Image] = deriveDecoder
    implicit lazy val imageAssetDecoder: Decoder[ImageAsset] = deriveDecoder
    implicit lazy val imageAssetDimensionsDecoder: Decoder[ImageAssetDimensions] = deriveDecoder
    implicit lazy val newspaperDecoder: Decoder[Newspaper] = deriveDecoder
    implicit lazy val notificationProvidersDecoder: Decoder[NotificationProviders] = deriveDecoder
    implicit lazy val referenceDecoder: Decoder[Reference] = deriveDecoder
    implicit lazy val sectionDecoder: Decoder[Section] = deriveDecoder
    implicit lazy val tagDecoder: Decoder[Tag] = deriveDecoder
    implicit lazy val tagUsageDecoder: Decoder[TagUsage] = deriveDecoder
    implicit lazy val taxonomyDecoder: Decoder[Taxonomy] = deriveDecoder
    implicit lazy val userDecoder: Decoder[User] = deriveDecoder

    implicit lazy val atomDecoder: Decoder[Atom] = deriveDecoder
  }


  /**
   * A custom decoder for backwards-compatibility.
   *
   * The old format in dynamo did not include the type under the `data` union field. E.g.
   * Old format:
   * ```
   * {
   * data: {
   * items: []
   * ]
   * }
   * ```
   *
   * New format:
   * ```
   * {
   * data: {
   * profile: {
   * items: []
   * }
   * }
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

          //Special case for commonsDivision because the scrooge enum value loses the casing
          if (data(nestedDataFieldName).nonEmpty || nestedDataFieldName == "commonsdivision") c.value
          else {
            //Add the union type name under `data`
            val newData = JsonObject.fromIterable(Seq(nestedDataFieldName -> dataJson))
            Json.fromJsonObject(topLevelObj.add("data", Json.fromJsonObject(newData)))
          }
        }

        fixedJson.as[Atom](decoders.atomDecoder)
      }

      result.getOrElse(Left(DecodingFailure("Json does not match Atom format", c.history)))
    }
  }
}
