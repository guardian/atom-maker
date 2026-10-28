package com.gu.atom.publish.test

import java.io.ByteArrayInputStream

import com.gu.atom.TestData.testAtoms
import com.gu.atom.publish.ThriftSerializer
import com.gu.contentatom.thrift.{ContentAtomEvent, EventType}
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.transport.TIOStreamTransport
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ThriftSerializerSpec extends AnyFunSpec with Matchers {
  describe("Thrift serializer") {
    testAtoms.foreach { atom =>
      it(s"should round-trip atom ${atom.id} using the Scrooge codec and compact protocol") {
        val event = ContentAtomEvent(atom, EventType.Update, 123456789L)
        val serializer = new ThriftSerializer[ContentAtomEvent] {}
        val serialized = serializer.serializeEvent(event)
        val bytes = new Array[Byte](serialized.remaining())
        serialized.get(bytes)
        val input = new ByteArrayInputStream(bytes)

        input.read() should equal(0)
        val protocol = new TCompactProtocol(new TIOStreamTransport(input))
        ContentAtomEvent.decode(protocol) should equal(event)
        input.available() should equal(0)
      }
    }
  }
}
