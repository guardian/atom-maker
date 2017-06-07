package com.gu.atom.data

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift.AtomData
import com.gu.contentatom.thrift.AtomData._
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.explainer.ExplainerAtom
import com.gu.contentatom.thrift.atom.interactive.InteractiveAtom
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.gu.contentatom.thrift.atom.quiz.QuizAtom
import com.gu.contentatom.thrift.atom.recipe.RecipeAtom
import com.gu.contentatom.thrift.atom.review.ReviewAtom
import com.gu.contentatom.thrift.atom.storyquestions.StoryQuestionsAtom
import com.gu.contentatom.thrift.atom.qanda.QAndAAtom
import com.gu.contentatom.thrift.atom.guide.GuideAtom
import com.gu.contentatom.thrift.atom.profile.ProfileAtom
import com.gu.contentatom.thrift.atom.timeline.TimelineAtom
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}

trait AtomDynamoFormats {
  implicit val dynamoFormat: DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(t: AtomData) = t match {
      case Quiz(d)            => writeAtom[QuizAtom](d)
      case Media(d)           => writeAtom[MediaAtom](d)
      case Explainer(d)       => writeAtom[ExplainerAtom](d)
      case Cta(d)             => writeAtom[CTAAtom](d)
      case Interactive(d)     => writeAtom[InteractiveAtom](d)
      case Review(d)          => writeAtom[ReviewAtom](d)
      case Recipe(d)          => writeAtom[RecipeAtom](d)
      case Storyquestions(d)  => writeAtom[StoryQuestionsAtom](d)
      case Guide(d)           => writeAtom[GuideAtom](d)
      case Profile(d)         => writeAtom[ProfileAtom](d)
      case Qa(d)              => writeAtom[QAndAAtom](d)
      case Timeline(d)        => writeAtom[TimelineAtom](d)
      case UnknownUnionField(_) => throw new RuntimeException("Unknown atom data type found.")
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

  private def writeAtom[A <: ThriftStruct](atomData: A)(implicit f: DynamoFormat[A]): AttributeValue = f.write(atomData)

  private def readAtom[A <: ThriftStruct](av: AttributeValue)(implicit f: DynamoFormat[A]): Either[DynamoReadError, A] = f.read(av)
}
