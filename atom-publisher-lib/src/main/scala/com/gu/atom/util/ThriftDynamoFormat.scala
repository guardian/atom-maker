package com.gu.atom.util

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.DynamoReadError
import com.twitter.scrooge.ThriftStruct
import collection.JavaConverters._
import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

trait ThriftDynamoFormat[T] extends DynamoFormat[T] {
  def read(av: AttributeValue): Either[DynamoReadError, T]
  def write(t: T): AttributeValue
}

object ThriftDynamoFormat {
  implicit val hnil: ThriftDynamoFormat[HNil] = new ThriftDynamoFormat[HNil] {
    def read(av: AttributeValue): Either[DynamoReadError, HNil] =
      Right(HNil)
    def write(t: HNil): AttributeValue = {
      var av = new AttributeValue
      av.setM(Map.empty[String, AttributeValue].asJava)
      av
    }
  }

  implicit def hcons[K <: Symbol, H, T <: HList](
    implicit
    witness: Witness.Aux[K],
    hInst: Lazy[DynamoFormat[H]],
    tInst: ThriftDynamoFormat[T]
  ): ThriftDynamoFormat[FieldType[K, H] :: T] = new ThriftDynamoFormat[FieldType[K, H] :: T] {
    val fieldName = witness.value.name
    def read(av: AttributeValue): Either[DynamoReadError, FieldType[K, H] :: T] = {
      val h: Either[DynamoReadError, H] = hInst.value.read(av)
      val t: Either[DynamoReadError, T] = tInst.read(av)
      t flatMap { t: T => h map { h: H => field[witness.T](h) :: t }}
    }
    def write(t: FieldType[K, H] :: T): AttributeValue = {
      val hv = hInst.value.write(t.head)
      val tv = tInst.write(t.tail)
      tv.addMEntry(fieldName, hv)
    }
  }

  implicit def genHListFormatter[T <: ThriftStruct, H <: HList](
    implicit
    gen: LabelledGeneric.Aux[T, H],
    rInst: Lazy[ThriftDynamoFormat[H]]
  ): ThriftDynamoFormat[T] = new ThriftDynamoFormat[T] {
    def read(av: AttributeValue): Either[DynamoReadError, T] = rInst.value.read(av) map gen.from
    def write(t: T): AttributeValue = rInst.value.write(gen.to(t))
  }
}
