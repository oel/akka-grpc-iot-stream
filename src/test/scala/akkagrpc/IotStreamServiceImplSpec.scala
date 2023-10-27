//#full-example
package akkagrpc

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Source, Sink}

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

import Util._

class GreeterServiceImplSpec
  extends AnyWordSpec
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system

  val service = new IotStreamServiceImpl(system)

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  "IotStreamServiceImpl" should {
    "produce a StatesUpdateResponse for each StatesUpdateRequest" in {
      val d = IotDevice.withRandomStates(1000)
      val updateRequest = StatesUpdateRequest(
          randomId(), "client1", 1000, d.deviceId, d.deviceType, d.timestamp, d.opState, d.setting
        )
      val requestStream = Source.single(updateRequest)
      val responseStream = service.sendIotUpdate(requestStream)
      val future = responseStream.runWith(Sink.seq[StatesUpdateResponse])
      val result = Await.result(future, 2.seconds)
      assert(result.size == 1)
    }
  }
}
