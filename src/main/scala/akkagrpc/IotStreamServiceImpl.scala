package akkagrpc

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl._

import scala.concurrent.duration._

import Util._

class IotStreamServiceImpl(system: ActorSystem[_]) extends IotStreamService {

  private implicit val sys: ActorSystem[_] = system

  val backpressureTMO: FiniteDuration = 3.seconds

  val updateIotFlow: Flow[StatesUpdateRequest, StatesUpdateResponse, NotUsed] =
    Flow[StatesUpdateRequest]
      .map { case StatesUpdateRequest(id, clientId, propId, devId, devType, ts, opState, setting, _) =>
        val (opStateNew, settingNew) =
          updateDeviceStates(propId, devId, devType, opState, setting)
        StatesUpdateResponse(
          randomId(),
          clientId,
          propId,
          devId,
          devType,
          System.currentTimeMillis(),
          opStateNew,
          settingNew)
        }

  val (inHub: Sink[StatesUpdateRequest, NotUsed], outHub: Source[StatesUpdateResponse, NotUsed]) =
    MergeHub.source[StatesUpdateRequest]
      .via(updateIotFlow)
      .toMat(BroadcastHub.sink[StatesUpdateResponse])(Keep.both)
      .run()

  val dynamicPubSubFlow: Flow[StatesUpdateRequest, StatesUpdateResponse, NotUsed] =
    Flow.fromSinkAndSource(inHub, outHub)

  @Override
  def sendIotUpdate(requests: Source[StatesUpdateRequest, NotUsed]): Source[StatesUpdateResponse, NotUsed] =
    requests.via(updateIotFlow).backpressureTimeout(backpressureTMO)

  @Override
  def broadcastIotUpdate(requests: Source[StatesUpdateRequest, NotUsed]): Source[StatesUpdateResponse, NotUsed] =
    requests.via(dynamicPubSubFlow).backpressureTimeout(backpressureTMO)

  def updateDeviceStates(propId: Int, devId: String, devType: Int, opState: Int, setting: Int): (Int, Int) = {
    // Random device states update simulating algorithmic adjustment in accordance with device and
    // property specific factors (temperature, lighting, etc)
    devType match {
      case 0 =>
        val opStateNew =
          if (opState == 0) randomInt(0, 3) else {
            if (opState == 1) randomInt(0, 2) else (2 + randomInt(0, 2)) % 3
          }
        val settingTemp = setting + randomInt(-2, 3)
        val settingNew = if (settingTemp < 60) 60 else if (settingTemp > 75) 75 else settingTemp
        (opStateNew, settingNew)
      case 1 =>
        (randomInt(0, 2), randomInt(1, 4))
      case 2 =>
        (randomInt(0, 2), randomInt(1, 6))
    }
  }
}
