package miwu.device

import miwu.support.base.MiwuDevice
import miwu.annotation.*
import miwu.widget.*

@Device("humidifier")
@Widgets(
    IntSeekbar::class,
    ModeButton::class,
    WidgetSwitch::class,
)
class Humidifier : MiwuDevice()
