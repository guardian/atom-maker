package com.gu.atom.util

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.DynamoReadError
import com.twitter.scrooge.ThriftStruct
import collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context
import shapeless._
import shapeless.labelled.FieldType
import shapeless.syntax.singleton._

trait ThriftDynamoFormat[T] extends DynamoFormat[T] {
  def read(av: AttributeValue): Either[DynamoReadError, T]
  def write(t: T): AttributeValue
}

object ThriftDynamoFormat {
  def apply[A](implicit f: ThriftDynamoFormat[A]): ThriftDynamoFormat[A] = f

  // Thanks Travis Brown: https://github.com/travisbrown/scrooge-circe-demo/blob/master/src/main/scala/demo/package.scala
  implicit def materializeStructGen[A]: LabelledGeneric[A] =
    macro materializeStructGen_impl[A]

  def materializeStructGen_impl[A: c.WeakTypeTag](c: Context): c.Tree = {
    import c.universe._

    val A = weakTypeOf[A]
    val I = A.companion.member(TypeName("Immutable")) match {
      case NoSymbol => c.abort(c.enclosingPosition, "Not a valid Scrooge class")
      case symbol => symbol.asType.toType
    }
    val N = appliedType(typeOf[NoPassthrough[_, _]].typeConstructor, A, I)

    q"""{
      val np = _root_.shapeless.the[$N]
      new _root_.shapeless.LabelledGeneric[$A] {
        type Repr = np.Without
        def to(t: $A): Repr = np.to(t.copy().asInstanceOf[$I])
        def from(r: Repr): $A = np.from(r)
      }
    }"""
  }

  implicit val hnil: ThriftDynamoFormat[HNil] = new ThriftDynamoFormat[HNil] {
    def read(av: AttributeValue): Either[DynamoReadError, HNil] = Right(HNil): Either[DynamoReadError, HNil]
    def write(t: HNil): AttributeValue = {
      var av = new AttributeValue
      av.setM(Map.empty[String, AttributeValue].asJava)
      av
    }
  }

  implicit def hcons[K <: Symbol, H, T <: HList](
    implicit
    witness: Witness.Aux[K],
    hInst: Lazy[ThriftDynamoFormat[H]],
    tInst: ThriftDynamoFormat[T]
  ): ThriftDynamoFormat[FieldType[K, H] :: T] = new ThriftDynamoFormat[FieldType[K, H] :: T] {
    def read(av: AttributeValue): Either[DynamoReadError, FieldType[K, H] :: T] = {
      val h: Either[DynamoReadError, H] = hInst.value.read(av)
      val t: Either[DynamoReadError, T] = tInst.read(av)
      t flatMap { t: T => h map { h: H => (witness.value ->> h) :: t }}
    }
    def write(t: FieldType[K, H] :: T): AttributeValue = {
      val hv = hInst.value.write(t.head)
      val tv = tInst.write(t.tail)
      tv.addMEntry(witness.value.name, hv)
    }
  }

  implicit def genFormatter[T <: ThriftStruct, Repr](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    rInst: ThriftDynamoFormat[Repr]
  ) = new ThriftDynamoFormat[T] {
    def read(av: AttributeValue): Either[DynamoReadError, T] = rInst.read(av) map gen.from
    def write(t: T): AttributeValue = rInst.write(gen.to(t))
  }
}
