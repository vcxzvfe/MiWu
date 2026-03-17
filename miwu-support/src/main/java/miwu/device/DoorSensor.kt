package miwu.device

import miwu.support.base.MiwuDevice
import miwu.annotation.*
import miwu.widget.*

@Device("magnet-sensor")
@Widgets(Text::class)
class DoorSensor : MiwuDevice()
