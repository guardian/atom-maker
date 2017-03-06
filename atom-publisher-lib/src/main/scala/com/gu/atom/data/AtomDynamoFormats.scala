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
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.{DynamoReadError, TypeCoercionError}
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._

trait AtomDynamoFormats {
  implicit val dynamoFormat: DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(t: AtomData) = t match {
      case Quiz(_) => quizFormat.write(t)
      case Media(_) => mediaFormat.write(t)
      case Explainer(_) => explainerFormat.write(t)
      case Cta(_) => ctaFormat.write(t)
      case Interactive(_) => interactiveFormat.write(t)
      case Review(_) => reviewFormat.write(t)
      case Recipe(_) => recipeFormat.write(t)
      case Storyquestions(_) => storyquestionsFormat.write(t)
      case UnknownUnionField(_) => throw new RuntimeException("Unknown atom data type found.")
    }

    def read(av: AttributeValue): Either[DynamoReadError, AtomData] = {
      allFormats.map(_.read(av)).collectFirst { case succ@Right(_) => succ }.getOrElse(Left(TypeCoercionError(new RuntimeException(s"No dynamo format to read $av"))))
    }
  }

  private val allFormats: List[DynamoFormat[AtomData]] = List(mediaFormat, ctaFormat, recipeFormat, storyquestionsFormat)

  private def fallback(atomData: AtomData): AttributeValue = new AttributeValue().withS(s"unknown atom data type $atomData")

  private def quizFormat(implicit arg0: DynamoFormat[QuizAtom]): DynamoFormat[AtomData] = ???

  private def mediaFormat(implicit arg0: DynamoFormat[MediaAtom]): DynamoFormat[AtomData] = {
    def fromAtomData: PartialFunction[AtomData, MediaAtom] = {
      case AtomData.Media(data) => data
    }
    def toAtomData(data: MediaAtom): AtomData = AtomData.Media(data)

    new DynamoFormat[AtomData] {
      def write(atomData: AtomData): AttributeValue = {
        val pf = fromAtomData andThen { case data: MediaAtom => arg0.write(data) }
        pf.applyOrElse(atomData, fallback)
      }

      def read(attr: AttributeValue) = arg0.read(attr) map toAtomData
    }
  }

  private def explainerFormat(implicit arg0: DynamoFormat[ExplainerAtom]): DynamoFormat[AtomData] = ???

  private def ctaFormat(implicit arg0: DynamoFormat[CTAAtom]): DynamoFormat[AtomData] = {
    def fromAtomData: PartialFunction[AtomData, CTAAtom] = {
      case AtomData.Cta(data) => data
    }
    def toAtomData(data: CTAAtom): AtomData = AtomData.Cta(data)

    new DynamoFormat[AtomData] {
      def write(atomData: AtomData): AttributeValue = {
        val pf = fromAtomData andThen { case data: CTAAtom => arg0.write(data) }
        pf.applyOrElse(atomData, fallback)
      }

      def read(attr: AttributeValue) = arg0.read(attr) map toAtomData
    }
  }

  private def interactiveFormat(implicit arg0: DynamoFormat[InteractiveAtom]): DynamoFormat[AtomData] = ???

  private def reviewFormat(implicit arg0: DynamoFormat[ReviewAtom]): DynamoFormat[AtomData] = ???

  private def recipeFormat(implicit arg0: DynamoFormat[RecipeAtom]): DynamoFormat[AtomData] = {
    def fromAtomData: PartialFunction[AtomData, RecipeAtom] = {
      case AtomData.Recipe(data) => data
    }
    def toAtomData(data: RecipeAtom): AtomData = AtomData.Recipe(data)

    new DynamoFormat[AtomData] {
      def write(atomData: AtomData): AttributeValue = {
        val pf = fromAtomData andThen { case data: RecipeAtom => arg0.write(data) }
        pf.applyOrElse(atomData, fallback)
      }

      def read(attr: AttributeValue) = arg0.read(attr) map toAtomData
    }
  }

  private def storyquestionsFormat(implicit arg0: DynamoFormat[StoryQuestionsAtom]): DynamoFormat[AtomData] = {
    def fromAtomData: PartialFunction[AtomData, StoryQuestionsAtom] = {
      case AtomData.Storyquestions(data) => data
    }
    def toAtomData(data: StoryQuestionsAtom): AtomData = AtomData.Storyquestions(data)

    new DynamoFormat[AtomData] {
      def write(atomData: AtomData): AttributeValue = {
        val pf = fromAtomData andThen { case data: StoryQuestionsAtom => arg0.write(data) }
        pf.applyOrElse(atomData, fallback)
      }

      def read(attr: AttributeValue) = arg0.read(attr) map toAtomData
    }
  }
}
