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
      case Guide(_) => guideFormat.write(t)
      case Profile(_) => profileFormat.write(t)
      case Qa(_) => qaFormat.write(t)
      case Timeline(_) => timelineFormat.write(t)
      case UnknownUnionField(_) => throw new RuntimeException("Unknown atom data type found.")
    }
    def read(av: AttributeValue): Either[DynamoReadError, AtomData] =
      allFormats.map(_.read(av)).collectFirst { case success@Right(_) => success }
        .getOrElse(Left(TypeCoercionError(new RuntimeException(s"No dynamo format to read $av"))))
  }

  private val allFormats: List[DynamoFormat[AtomData]] = List(quizFormat, mediaFormat, explainerFormat, ctaFormat, interactiveFormat, reviewFormat, recipeFormat, storyquestionsFormat, qaFormat, profileFormat, guideFormat, timelineFormat)

  private def fallback(atomData: AtomData): AttributeValue = new AttributeValue().withS(s"unknown atom data type $atomData")

  private def quizFormat(implicit arg0: DynamoFormat[QuizAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Quiz(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Quiz] = arg0.read(attr).map(AtomData.Quiz(_))
  }

  private def mediaFormat(implicit arg0: DynamoFormat[MediaAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Media(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Media] = arg0.read(attr).map(AtomData.Media(_))
  }

  private def explainerFormat(implicit arg0: DynamoFormat[ExplainerAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Explainer(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Explainer] = arg0.read(attr).map(AtomData.Explainer(_))
  }

  private def ctaFormat(implicit arg0: DynamoFormat[CTAAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Cta(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Cta] = arg0.read(attr).map(AtomData.Cta(_))
  }

  private def interactiveFormat(implicit arg0: DynamoFormat[InteractiveAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Interactive(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Interactive] = arg0.read(attr).map(AtomData.Interactive(_))
  }

  private def reviewFormat(implicit arg0: DynamoFormat[ReviewAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Review(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Review] = arg0.read(attr).map(AtomData.Review(_))
  }

  private def recipeFormat(implicit arg0: DynamoFormat[RecipeAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Recipe(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Recipe] = arg0.read(attr).map(AtomData.Recipe(_))
  }

  private def storyquestionsFormat(implicit arg0: DynamoFormat[StoryQuestionsAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Storyquestions(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Storyquestions] = arg0.read(attr).map(AtomData.Storyquestions(_))
  }

  private def qaFormat(implicit arg0: DynamoFormat[QAndAAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Qa(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Qa] = arg0.read(attr).map(AtomData.Qa(_))
  }

  private def guideFormat(implicit arg0: DynamoFormat[GuideAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Guide(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Guide] = arg0.read(attr).map(AtomData.Guide(_))
  }

  private def profileFormat(implicit arg0: DynamoFormat[ProfileAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Profile(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Profile] = arg0.read(attr).map(AtomData.Profile(_))
  }

  private def timelineFormat(implicit arg0: DynamoFormat[TimelineAtom]): DynamoFormat[AtomData] = new DynamoFormat[AtomData] {
    def write(atomData: AtomData): AttributeValue = {
      val pf: PartialFunction[AtomData, AttributeValue] = { case AtomData.Timeline(data) => arg0.write(data) }
      pf.applyOrElse(atomData, fallback)
    }
    def read(attr: AttributeValue): Either[DynamoReadError, Timeline] = arg0.read(attr).map(AtomData.Timeline(_))
  }
}
