package com.gu.atom.util

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.DynamoReadError
import com.twitter.scrooge.ThriftEnum
import collection.JavaConverters._
import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

trait ThriftEnumDynamoFormat[T] extends DynamoFormat[T] {
  def read(av: AttributeValue): Either[DynamoReadError, T]
  def write(t: T): AttributeValue
}

object ThriftEnumDynamoFormat {
  implicit val henum: ThriftEnumDynamoFormat[String :: HNil] = new ThriftEnumDynamoFormat[String :: HNil] {
    def read(av: AttributeValue): Either[DynamoReadError, String :: HNil] =
      Right(av.getS :: HNil)
    def write(t: String :: HNil): AttributeValue =
      new AttributeValue().withS(t.head)
  }

  implicit def genHListFormatter[T <: ThriftEnum](
    implicit
    gen: Generic[T]
  ): ThriftEnumDynamoFormat[T] = new ThriftEnumDynamoFormat[T] {
    def read(av: AttributeValue): Either[DynamoReadError, T] = henum.read(av) map gen.from
    def write(t: T): AttributeValue = henum.write(gen.to(t))
  }
}
