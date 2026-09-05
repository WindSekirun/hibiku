package com.github.windsekirun.musicwidget.domain.model

data class WidgetConfig(
    val ringStyle: RingStyle = RingStyle.SOLID_CLASSIC,
    val borderColorHex: String = "#FFFFFF",
    val useDynamicColor: Boolean = true,
    val textVisible: Boolean = true,
    val shapeStyle: M3ShapeStyle = M3ShapeStyle.CIRCLE
)
