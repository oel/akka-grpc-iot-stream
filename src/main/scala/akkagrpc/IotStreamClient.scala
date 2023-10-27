package akkagrpc

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import akka.stream.ThrottleMode.Shaping
import akka.grpc.GrpcClientSettings

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

import Util._

// akkagrpc.IotStreamClient clientId broadcastYN propIdStart propIdEnd
//   e.g. akkagrpc.IotStreamClient client1 1 1000 1049  # broadcastYN: 1=Yes | 0=No
object IotStreamClient {

  def getDevicesByProperty(propId: Int): Iterator[IotDevice] =
    (1 to randomInt(1, 5)).map { _ =>  // 1-4 devices per property
        IotDevice.withRandomStates(propId)
      }.iterator

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "IotStreamClient")
    implicit val ec: ExecutionContext = sys.executionContext

    val client = IotStreamServiceClient(GrpcClientSettings.fromConfig("akkagrpc.IotStreamService"))

    val (clientId: String, broadcastYN: Int, propIdStart: Int, propIdEnd: Int) =
      if (args.length == 4) {
        Try((args(0), args(1).toInt, args(2).toInt, args(3).toInt)) match {
          case Success((cid, bcast, pid1, pid2)) =>
            (cid, bcast, pid1, pid2)
          case _ =>
            Console.err.println("[Main] ERROR: Arguments required: clientId, broadcast(Y=1|N=0), propIdStart & propIdEnd  e.g. client1 1 1000 1049")
            System.exit(1)
        }
      }
      else
        ("client1", 0, 1000, 1029)  // Default clientId, no-broadcast & property id range (inclusive)

    val devices: Iterator[IotDevice] =
      (propIdStart to propIdEnd).flatMap(getDevicesByProperty).iterator

    Console.out.println(s"Performing streaming requests from $clientId ...")
    grpcStreaming(clientId)

    def grpcStreaming(clientId: String): Unit = {
      val requestStream: Source[StatesUpdateRequest, NotUsed] =
        Source
          .fromIterator(() => devices)
          .map { case IotDevice(devId, devType, propId, ts, opState, setting) =>
            Console.out.println(s"[$clientId] REQUEST: $propId $devId ${DeviceType(devType)} | State: $opState, Setting: $setting")
            StatesUpdateRequest(randomId(), clientId, propId, devId, devType, ts, opState, setting)
          }
          .throttle(1, 100.millis, 10, Shaping)  // For illustration only

      val responseStream: Source[StatesUpdateResponse, NotUsed] = {
        if (broadcastYN == 0)
          client.sendIotUpdate(requestStream)
        else
          client.broadcastIotUpdate(requestStream)
      }

      val done: Future[Done] =
        responseStream.runForeach {
          case StatesUpdateResponse(id, clntId, propId, devId, devType, ts, opState, setting, _) =>
            Console.out.println(s"[$clientId] RESPONSE: [requester: $clntId] $propId $devId ${DeviceType(devType)} | State: $opState, Setting: $setting")
        }

      done.onComplete {
        case Success(_) =>
          Console.out.println(s"[$clientId] Done IoT states streaming.")
        case Failure(e) =>
          Console.err.println(s"[$clientId] ERROR: $e")
      }

      Thread.sleep(2000)
    }
  }
}
