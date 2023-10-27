package akkagrpc

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object Util {

  def randomInt(a: Int, b: Int): Int = ThreadLocalRandom.current().nextInt(a, b)

  def randomId(): String = UUID.randomUUID().toString.slice(0, 6)  // UUID's first 6 chars
}
