package com.gu.atom.data

import cats.syntax.either._
import cats.data.NonEmptyList
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.atom.facade.AtomFacade
import com.gu.atom.util.ThriftDynamoFormat
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.explainer.ExplainerAtom
import com.gu.contentatom.thrift.atom.guide.GuideAtom
import com.gu.contentatom.thrift.atom.interactive.InteractiveAtom
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.gu.contentatom.thrift.atom.profile.ProfileAtom
import com.gu.contentatom.thrift.atom.qanda.QAndAAtom
import com.gu.contentatom.thrift.atom.quiz.QuizAtom
import com.gu.contentatom.thrift.atom.recipe.RecipeAtom
import com.gu.contentatom.thrift.atom.review.ReviewAtom
import com.gu.contentatom.thrift.atom.storyquestions.StoryQuestionsAtom
import com.gu.contentatom.thrift.atom.timeline.TimelineAtom
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.{DynamoReadError, InvalidPropertiesError, MissingProperty, PropertyReadError}
import com.twitter.scrooge.ThriftStruct

import scala.collection.JavaConverters._

trait AtomDynamoFormats {
  import AtomFacade._
  import ThriftDynamoFormat._

  implicit val atomFormat: ThriftDynamoFormat[Atom] = new ThriftDynamoFormat[Atom] {
    override def write(t: Atom): AttributeValue = {
      val required: Map[String, AttributeValue] = Map(
        "id" -> writeValue(t.id),
        "atomType" -> writeValue(t.atomType),
        "labels" -> writeValue(t.labels),
        "defaultHtml" -> writeValue(t.defaultHtml),
        "data" -> writeData(t.data),
        "contentChangeDetails" -> writeValue(t.contentChangeDetails)
      )

      val optional = List(
        t.flags.map { flags => "flags" -> writeValue(flags) },
        t.title.map { title => "title" -> writeValue(title) }
      ).flatten.toMap

      new AttributeValue().withM((required ++ optional).asJava)
    }

    override def read(av: AttributeValue): Either[DynamoReadError, Atom] = {
      val attrs = av.getM

      for {
        id <- readField[String]("id", attrs)
        atomType <- readField[AtomType]("atomType", attrs)
        labels <- readField[Seq[String]]("labels", attrs)
        defaultHtml <- readField[String]("defaultHtml", attrs)
        data <- readDataField(atomType, attrs)
        contentChangeDetails <- readField[ContentChangeDetails]("contentChangeDetail", attrs)
      } yield Atom(
        id, atomType, labels, defaultHtml, data, contentChangeDetails
      )
    }

    private def writeData(t: AtomData): AttributeValue = t match {
      case AtomData.Quiz(d)            => writeStruct[QuizAtom](d)
      case AtomData.Media(d)           => writeStruct[MediaAtom](d)
      case AtomData.Explainer(d)       => writeStruct[ExplainerAtom](d)
      case AtomData.Cta(d)             => writeStruct[CTAAtom](d)
      case AtomData.Interactive(d)     => writeStruct[InteractiveAtom](d)
      case AtomData.Review(d)          => writeStruct[ReviewAtom](d)
      case AtomData.Recipe(d)          => writeStruct[RecipeAtom](d)
      case AtomData.Storyquestions(d)  => writeStruct[StoryQuestionsAtom](d)
      case AtomData.Guide(d)           => writeStruct[GuideAtom](d)
      case AtomData.Profile(d)         => writeStruct[ProfileAtom](d)
      case AtomData.Qanda(d)           => writeStruct[QAndAAtom](d)
      case AtomData.Timeline(d)        => writeStruct[TimelineAtom](d)
      case _                           => throw new RuntimeException("Unknown atom data type found.")
    }

    private def readDataField(atomType: AtomType, attrs: java.util.Map[String, AttributeValue]): Either[DynamoReadError, AtomData] = {
      if(attrs.containsKey("data")) {
        val v = attrs.get("data")

        atomType match {
          case AtomType.Quiz => readStruct[AtomData.Quiz](v)
          case AtomType.Media => readStruct[AtomData.Media](v)
          case AtomType.Explainer => readStruct[AtomData.Explainer](v)
          case AtomType.Cta => readStruct[AtomData.Cta](v)
          case AtomType.Interactive => readStruct[AtomData.Interactive](v)
          case AtomType.Review => readStruct[AtomData.Review](v)
          case AtomType.Recipe => readStruct[AtomData.Recipe](v)
          case AtomType.Storyquestions => readStruct[AtomData.Storyquestions](v)
          case AtomType.Guide => readStruct[AtomData.Guide](v)
          case AtomType.Profile => readStruct[AtomData.Profile](v)
          case AtomType.Qanda => readStruct[AtomData.Qanda](v)
          case AtomType.Timeline => readStruct[AtomData.Timeline](v)
          case _ => throw new RuntimeException("Unknown atom data type found.")
        }
      } else {
        Left(missingProperty("data"))
      }
    }

    private def readField[T](key: String, attrs: java.util.Map[String, AttributeValue])(implicit fmt: DynamoFormat[T]): Either[DynamoReadError, T] = {
      if(attrs.containsKey(key)) {
        fmt.read(attrs.get(key))
      } else {
        Left(missingProperty(key))
      }
    }

    private def writeValue[A](v: A)(implicit f: DynamoFormat[A]): AttributeValue = f.write(v)
    private def readValue[A](av: AttributeValue)(implicit f: DynamoFormat[A]): Either[DynamoReadError, A] = f.read(av)

    private def writeStruct[A <: ThriftStruct](v: A)(implicit f: ThriftDynamoFormat[A]): AttributeValue = f.write(v)
    private def readStruct[A <: ThriftStruct](av: AttributeValue)(implicit f: ThriftDynamoFormat[A]): Either[DynamoReadError, A] = f.read(av)

    private def missingProperty(key: String): DynamoReadError = {
      InvalidPropertiesError(NonEmptyList.of(PropertyReadError(key, MissingProperty)))
    }
  }
}
