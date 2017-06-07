package com.gu.atom.data

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.atom.util._
import com.gu.contentatom.thrift.{ImageAsset, ImageAssetDimensions, Image}
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.explainer.{ExplainerAtom, DisplayType => EDisplayType}
import com.gu.contentatom.thrift.atom.interactive.InteractiveAtom
import com.gu.contentatom.thrift.atom.media.{MediaAtom, Platform, AssetType, Category, PrivacyStatus, Asset => MAsset, PlutoData => PData, Metadata}
import com.gu.contentatom.thrift.atom.quiz.{ResultGroup, Asset => QAsset, Answer, ResultBucket, ResultBuckets, Question => QQuestion, ResultGroups, QuizContent, QuizAtom}
import com.gu.contentatom.thrift.atom.recipe.{Tags, Time, Serves, Range, Ingredient, IngredientsList, RecipeAtom}
import com.gu.contentatom.thrift.atom.review.{Rating, ReviewAtom}
import com.gu.contentatom.thrift.atom.storyquestions.{Question => SQuestion, QuestionSet => SQuestionSet, StoryQuestionsAtom}
import com.gu.contentatom.thrift.atom.qanda.{QAndAItem, QAndAAtom}
import com.gu.contentatom.thrift.atom.guide.{GuideItem, GuideAtom}
import com.gu.contentatom.thrift.atom.profile.{ProfileItem, ProfileAtom}
import com.gu.contentatom.thrift.atom.timeline.{TimelineItem, TimelineAtom}
import com.gu.contentatom.thrift.AtomData
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol.TProtocol

trait AtomDynamoFormats {
  // summon implicits for atom parts
  DynamoFormat[ImageAssetDimensions]
  DynamoFormat[ImageAsset]
  DynamoFormat[Image]
  DynamoFormat[EDisplayType]
  DynamoFormat[Platform]
  DynamoFormat[AssetType]
  DynamoFormat[Category]
  DynamoFormat[PrivacyStatus]
  DynamoFormat[PData]
  DynamoFormat[Metadata]
  DynamoFormat[ResultGroup]
  DynamoFormat[QAsset]
  DynamoFormat[Answer]
  DynamoFormat[ResultBucket]
  DynamoFormat[ResultBuckets]
  DynamoFormat[QQuestion]
  DynamoFormat[ResultGroups]
  DynamoFormat[QuizContent]
  DynamoFormat[Tags]
  DynamoFormat[Time]
  DynamoFormat[Serves]
  DynamoFormat[Range]
  DynamoFormat[Ingredient]
  DynamoFormat[IngredientsList]
  DynamoFormat[Rating]
  DynamoFormat[SQuestion]
  DynamoFormat[SQuestionSet]
  DynamoFormat[QAndAItem]
  DynamoFormat[GuideItem]
  DynamoFormat[ProfileItem]
  DynamoFormat[TimelineItem]

  private def writeAtom[A <: ThriftStruct](atomData: A)(implicit f: DynamoFormat[A]): AttributeValue = f.write(atomData)

  private def readAtom[A <: ThriftStruct](av: AttributeValue)(implicit f: DynamoFormat[A]): Either[DynamoReadError, A] = f.read(av)

  private val allReaders = List(
    readAtom[QuizAtom] _,
    readAtom[MediaAtom] _,
    readAtom[ExplainerAtom] _,
    readAtom[CTAAtom] _,
    readAtom[InteractiveAtom] _,
    readAtom[ReviewAtom] _,
    readAtom[RecipeAtom] _,
    readAtom[StoryQuestionsAtom] _,
    readAtom[QAndAAtom] _,
    readAtom[GuideAtom] _,
    readAtom[ProfileAtom] _,
    readAtom[TimelineAtom] _
  )

  implicit val dynamoFormat: DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(t: AtomData) = t match {
      case AtomData.Quiz(d)            => writeAtom[QuizAtom](d)
      case AtomData.Media(d)           => writeAtom[MediaAtom](d)
      case AtomData.Explainer(d)       => writeAtom[ExplainerAtom](d)
      case AtomData.Cta(d)             => writeAtom[CTAAtom](d)
      case AtomData.Interactive(d)     => writeAtom[InteractiveAtom](d)
      case AtomData.Review(d)          => writeAtom[ReviewAtom](d)
      case AtomData.Recipe(d)          => writeAtom[RecipeAtom](d)
      case AtomData.Storyquestions(d)  => writeAtom[StoryQuestionsAtom](d)
      case AtomData.Guide(d)           => writeAtom[GuideAtom](d)
      case AtomData.Profile(d)         => writeAtom[ProfileAtom](d)
      case AtomData.Qa(d)              => writeAtom[QAndAAtom](d)
      case AtomData.Timeline(d)        => writeAtom[TimelineAtom](d)
      case AtomData.UnknownUnionField(_) => throw new RuntimeException("Unknown atom data type found.")
    }

    def read(av: AttributeValue): Either[DynamoReadError, AtomData] = {
      val zero: Either[DynamoReadError, ThriftStruct] = Either.left(TypeCoercionError(new RuntimeException(s"No dynamo format to read $av")))
      allReaders.foldLeft(zero) {
        case (Left(_), reader) => reader(av)
        case (res, _)          => res
      }.map {
        case d: QuizAtom           => AtomData.Quiz(d)
        case d: MediaAtom          => AtomData.Media(d)
        case d: ExplainerAtom      => AtomData.Explainer(d)
        case d: CTAAtom            => AtomData.Cta(d)
        case d: InteractiveAtom    => AtomData.Interactive(d)
        case d: ReviewAtom         => AtomData.Review(d)
        case d: RecipeAtom         => AtomData.Recipe(d)
        case d: StoryQuestionsAtom => AtomData.Storyquestions(d)
        case d: GuideAtom          => AtomData.Guide(d)
        case d: ProfileAtom        => AtomData.Profile(d)
        case d: QAndAAtom          => AtomData.Qa(d)
        case d: TimelineAtom       => AtomData.Timeline(d)
        case _                     => throw new RuntimeException("Unknown atom data type found.")
      }
    }
  }
}
