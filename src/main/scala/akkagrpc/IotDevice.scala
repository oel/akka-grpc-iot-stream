package akkagrpc

import Util._
/*
deviceType:
  0 -> Thermostat | 1 -> Lamp | 2 -> SecurityAlarm
opState:
  devType 0 -> 0|1|2 (OFF|HEAT|COOL)
  devType 1 -> 0|1 (OFF|ON)
  devType 2 -> 0|1 (OFF|ON)
setting:
  devType 0 -> 60-75
  devType 1 -> 1-3
  devType 2 -> 1-5
*/

object DeviceType extends Enumeration {
  type DeviceType = Value
  val Thermostat: Value = Value(0)
  val Lamp: Value = Value(1)
  val SecurityAlarm: Value = Value(2)
}

case class IotDevice( deviceId: String,
                      deviceType: Int,
                      propertyId: Int,
                      timestamp: Long,
                      opState: Int,
                      setting: Int )

object IotDevice {

  def withRandomStates(propertyId: Int): IotDevice = {
    val devType = randomInt(0, 3)  // 0 -> Thermostat | 1 -> Lamp | 2 -> SecurityAlarm
    val (opState: Int, setting: Int) = devType match {
      case 0 => (randomInt(0, 3), randomInt(60, 76))  // 0|1|2 (OFF|HEAT|COOL), 60-75
      case 1 => (randomInt(0, 2), randomInt(1, 4))  // 0|1 (OFF|ON), 1-3
      case 2 => (randomInt(0, 2), randomInt(1, 6))  // 0|1 (OFF|ON), 1-5
    }
    IotDevice(
      randomId(),
      devType,
      propertyId,
      System.currentTimeMillis(),
      opState,
      setting
    )
  }
}
