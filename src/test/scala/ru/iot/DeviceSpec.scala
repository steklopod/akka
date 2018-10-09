package ru.iot

import akka.testkit.TestProbe
import ru.testkit.AkkaSpec

class DeviceSpec extends AkkaSpec {
  val probe       = TestProbe()
  val deviceActor = system.actorOf(Device.props("group", "device"))

  "Device actor" must {
    "reply with empty reading if no temperature is known" in {
      deviceActor tell (Device.ReadTemperature(requestId = 42), probe.ref)
      val response: Device.RespondTemperature = probe.expectMsgType[Device.RespondTemperature]

      response.requestId should ===(42)
      response.value     should ===(None)
    }

    "reply with latest temperature reading" in {
      deviceActor.tell(Device.RecordTemperature(requestId = 1, 24.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 1))

      deviceActor.tell(Device.ReadTemperature(requestId = 2), probe.ref)
      val response1 = probe.expectMsgType[Device.RespondTemperature]
      response1.requestId should ===(2)
      response1.value     should ===(Some(24.0))

      deviceActor.tell(Device.RecordTemperature(requestId = 3, 55.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 3))

      deviceActor.tell(Device.ReadTemperature(requestId = 4), probe.ref)
      val response2 = probe.expectMsgType[Device.RespondTemperature]
      response2.requestId should ===(4)
      response2.value     should ===(Some(55.0))
    }

  }

}
