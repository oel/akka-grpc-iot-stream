package akkagrpc

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Source, Sink}
import akka.grpc.GrpcClientSettings

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

import Util._

class IotStreamSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  val testKit = ActorTestKit(conf)

  val serverSystem: ActorSystem[_] = testKit.system
  val bound = new IotStreamServer(serverSystem).run()

  bound.futureValue

  implicit val clientSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "IotStreamClient")

  val client = IotStreamServiceClient(GrpcClientSettings.fromConfig("akkagrpc.IotStreamService"))

  override def afterAll(): Unit = {
    ActorTestKit.shutdown(clientSystem)
    testKit.shutdownTestKit()
  }

  "IotStreamServiceImpl" should {
    "produce a StatesUpdateResponse for each StatesUpdateRequest" in {
      val d = IotDevice.withRandomStates(1000)
      val updateRequest = StatesUpdateRequest(
          randomId(), "client1", 1000, d.deviceId, d.deviceType, d.timestamp, d.opState, d.setting
        )
      val requestStream = Source.single(updateRequest)
      val responseStream = client.sendIotUpdate(requestStream)
      val future = responseStream.runWith(Sink.seq[StatesUpdateResponse])
      val result = Await.result(future, 2.seconds)
      assert(result.size == 1)
    }
  }
}
