package com.example.smart_home_wallpad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class WallpadIconType {
    LIGHT, CURTAIN, CAMERA, PLUG, THERMOMETER, DROPLET, HOME, AUTOMATION, SETTINGS,
    OUTING, SLEEP, EXERCISE, TABLE_LAMP, CEILING_LIGHT, POWER_STRIP, INDOOR_ENVIRONMENT,
    TEMPERATURE, HUMIDITY,
}

@Composable
fun WallpadIcon(
    type: WallpadIconType,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color(0xFF4D5C68),
) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.095f)
        val c = Offset(size.width / 2f, size.height / 2f)
        when (type) {
            WallpadIconType.LIGHT -> {
                drawCircle(tint, radius = size.minDimension * .19f, center = c, style = stroke)
                listOf(Offset(0f, -.37f), Offset(0f, .37f), Offset(-.37f, 0f), Offset(.37f, 0f)).forEach { d ->
                    val start = c + d * size.minDimension
                    val end = c + d * (size.minDimension * .72f)
                    drawLine(tint, start, end, stroke.width)
                }
            }
            WallpadIconType.CAMERA -> {
                drawRoundRect(tint, Offset(size.width * .12f, size.height * .28f), Size(size.width * .58f, size.height * .46f), CornerRadius(size.minDimension * .08f), style = stroke)
                drawCircle(tint, size.minDimension * .12f, c, style = stroke)
                val path = Path().apply { moveTo(size.width * .72f, size.height * .40f); lineTo(size.width * .91f, size.height * .29f); lineTo(size.width * .91f, size.height * .71f); lineTo(size.width * .72f, size.height * .60f); close() }
                drawPath(path, tint, style = stroke)
            }
            WallpadIconType.PLUG -> {
                drawRoundRect(tint, Offset(size.width * .28f, size.height * .28f), Size(size.width * .44f, size.height * .36f), CornerRadius(size.minDimension * .08f), style = stroke)
                drawLine(tint, Offset(size.width * .38f, size.height * .12f), Offset(size.width * .38f, size.height * .28f), stroke.width)
                drawLine(tint, Offset(size.width * .62f, size.height * .12f), Offset(size.width * .62f, size.height * .28f), stroke.width)
                drawLine(tint, Offset(size.width * .50f, size.height * .64f), Offset(size.width * .50f, size.height * .87f), stroke.width)
            }
            WallpadIconType.THERMOMETER -> {
                drawCircle(tint, size.minDimension * .15f, Offset(c.x, size.height * .72f), style = stroke)
                drawLine(tint, Offset(c.x, size.height * .20f), Offset(c.x, size.height * .62f), stroke.width)
                drawLine(tint, Offset(c.x - size.width * .13f, size.height * .20f), Offset(c.x + size.width * .13f, size.height * .20f), stroke.width)
            }
            WallpadIconType.DROPLET -> {
                val path = Path().apply { moveTo(c.x, size.height * .12f); cubicTo(size.width * .17f, size.height * .48f, size.width * .25f, size.height * .78f, c.x, size.height * .86f); cubicTo(size.width * .75f, size.height * .78f, size.width * .83f, size.height * .48f, c.x, size.height * .12f); close() }
                drawPath(path, tint, style = stroke)
            }
            WallpadIconType.INDOOR_ENVIRONMENT -> {
                val house = Path().apply {
                    moveTo(size.width * .14f, size.height * .47f)
                    lineTo(c.x, size.height * .16f)
                    lineTo(size.width * .86f, size.height * .47f)
                    lineTo(size.width * .74f, size.height * .47f)
                    lineTo(size.width * .74f, size.height * .83f)
                    lineTo(size.width * .26f, size.height * .83f)
                    lineTo(size.width * .26f, size.height * .47f)
                    close()
                }
                drawPath(house, tint, style = stroke)

                val droplet = Path().apply {
                    moveTo(c.x, size.height * .39f)
                    cubicTo(size.width * .40f, size.height * .55f, size.width * .42f, size.height * .67f, c.x, size.height * .71f)
                    cubicTo(size.width * .58f, size.height * .67f, size.width * .60f, size.height * .55f, c.x, size.height * .39f)
                    close()
                }
                drawPath(droplet, tint, style = stroke)
            }
            WallpadIconType.TEMPERATURE -> {
                drawRoundRect(
                    tint,
                    Offset(size.width * .38f, size.height * .13f),
                    Size(size.width * .24f, size.height * .54f),
                    CornerRadius(size.minDimension * .12f),
                    style = stroke,
                )
                drawCircle(tint, size.minDimension * .17f, Offset(c.x, size.height * .73f), style = stroke)
                drawLine(
                    tint,
                    Offset(c.x, size.height * .31f),
                    Offset(c.x, size.height * .72f),
                    stroke.width * 1.35f,
                )
                drawCircle(tint, size.minDimension * .10f, Offset(c.x, size.height * .73f))
                drawLine(tint, Offset(size.width * .69f, size.height * .27f), Offset(size.width * .80f, size.height * .27f), stroke.width)
                drawLine(tint, Offset(size.width * .69f, size.height * .43f), Offset(size.width * .77f, size.height * .43f), stroke.width)
            }
            WallpadIconType.HUMIDITY -> {
                val droplet = Path().apply {
                    moveTo(c.x, size.height * .10f)
                    cubicTo(size.width * .18f, size.height * .48f, size.width * .27f, size.height * .78f, c.x, size.height * .87f)
                    cubicTo(size.width * .73f, size.height * .78f, size.width * .82f, size.height * .48f, c.x, size.height * .10f)
                    close()
                }
                drawPath(droplet, tint, style = stroke)
                drawArc(
                    tint,
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(size.width * .33f, size.height * .46f),
                    size = Size(size.width * .34f, size.height * .23f),
                    style = stroke,
                )
            }
            WallpadIconType.CURTAIN -> {
                drawLine(tint, Offset(size.width * .12f, size.height * .18f), Offset(size.width * .88f, size.height * .18f), stroke.width)
                drawCircle(tint, size.minDimension * .075f, Offset(size.width * .10f, size.height * .18f), style = stroke)
                drawCircle(tint, size.minDimension * .075f, Offset(size.width * .90f, size.height * .18f), style = stroke)
                val leftCurtain = Path().apply {
                    moveTo(size.width * .20f, size.height * .21f)
                    lineTo(size.width * .20f, size.height * .80f)
                    cubicTo(size.width * .25f, size.height * .87f, size.width * .32f, size.height * .88f, size.width * .38f, size.height * .82f)
                    cubicTo(size.width * .35f, size.height * .68f, size.width * .33f, size.height * .60f, size.width * .36f, size.height * .54f)
                    cubicTo(size.width * .47f, size.height * .42f, size.width * .48f, size.height * .29f, size.width * .47f, size.height * .21f)
                    close()
                }
                val rightCurtain = Path().apply {
                    moveTo(size.width * .80f, size.height * .21f)
                    lineTo(size.width * .80f, size.height * .80f)
                    cubicTo(size.width * .75f, size.height * .87f, size.width * .68f, size.height * .88f, size.width * .62f, size.height * .82f)
                    cubicTo(size.width * .65f, size.height * .68f, size.width * .67f, size.height * .60f, size.width * .64f, size.height * .54f)
                    cubicTo(size.width * .53f, size.height * .42f, size.width * .52f, size.height * .29f, size.width * .53f, size.height * .21f)
                    close()
                }
                drawPath(leftCurtain, tint, style = stroke)
                drawPath(rightCurtain, tint, style = stroke)
                drawRoundRect(tint, Offset(size.width * .18f, size.height * .51f), Size(size.width * .20f, size.height * .10f), CornerRadius(size.minDimension * .04f), style = stroke)
                drawRoundRect(tint, Offset(size.width * .62f, size.height * .51f), Size(size.width * .20f, size.height * .10f), CornerRadius(size.minDimension * .04f), style = stroke)
            }
            WallpadIconType.HOME -> {
                val path = Path().apply { moveTo(size.width * .13f, size.height * .48f); lineTo(c.x, size.height * .15f); lineTo(size.width * .87f, size.height * .48f); lineTo(size.width * .76f, size.height * .48f); lineTo(size.width * .76f, size.height * .84f); lineTo(size.width * .24f, size.height * .84f); lineTo(size.width * .24f, size.height * .48f); close() }
                drawPath(path, tint, style = stroke)
            }
            WallpadIconType.AUTOMATION -> {
                val rows = listOf(.27f, .50f, .73f)
                val knobs = listOf(.36f, .67f, .46f)
                rows.forEachIndexed { index, y ->
                    drawLine(tint, Offset(size.width * .14f, size.height * y), Offset(size.width * .86f, size.height * y), stroke.width)
                    drawCircle(tint, size.minDimension * .10f, Offset(size.width * knobs[index], size.height * y), style = stroke)
                }
            }
            WallpadIconType.SETTINGS -> {
                drawCircle(tint, size.minDimension * .24f, c, style = stroke)
                drawCircle(tint, size.minDimension * .075f, c, style = stroke)
                listOf(
                    Offset(.50f, .07f) to Offset(.50f, .20f),
                    Offset(.93f, .50f) to Offset(.80f, .50f),
                    Offset(.50f, .93f) to Offset(.50f, .80f),
                    Offset(.07f, .50f) to Offset(.20f, .50f),
                    Offset(.20f, .20f) to Offset(.29f, .29f),
                    Offset(.80f, .20f) to Offset(.71f, .29f),
                    Offset(.80f, .80f) to Offset(.71f, .71f),
                    Offset(.20f, .80f) to Offset(.29f, .71f),
                ).forEach { (start, end) ->
                    drawLine(
                        tint,
                        Offset(size.width * start.x, size.height * start.y),
                        Offset(size.width * end.x, size.height * end.y),
                        stroke.width,
                    )
                }
            }
            WallpadIconType.OUTING -> {
                drawRoundRect(
                    tint,
                    Offset(size.width * .16f, size.height * .31f),
                    Size(size.width * .68f, size.height * .51f),
                    CornerRadius(size.minDimension * .08f),
                    style = stroke,
                )
                drawRoundRect(
                    tint,
                    Offset(size.width * .36f, size.height * .16f),
                    Size(size.width * .28f, size.height * .18f),
                    CornerRadius(size.minDimension * .06f),
                    style = stroke,
                )
                drawLine(tint, Offset(size.width * .17f, size.height * .52f), Offset(size.width * .83f, size.height * .52f), stroke.width)
            }
            WallpadIconType.SLEEP -> {
                val path = Path().apply {
                    moveTo(size.width * .67f, size.height * .15f)
                    cubicTo(size.width * .40f, size.height * .23f, size.width * .30f, size.height * .58f, size.width * .51f, size.height * .76f)
                    cubicTo(size.width * .62f, size.height * .85f, size.width * .76f, size.height * .85f, size.width * .87f, size.height * .76f)
                    cubicTo(size.width * .61f, size.height * .80f, size.width * .48f, size.height * .45f, size.width * .67f, size.height * .15f)
                }
                drawPath(path, tint, style = stroke)
            }
            WallpadIconType.EXERCISE -> {
                drawLine(tint, Offset(size.width * .18f, c.y), Offset(size.width * .82f, c.y), stroke.width)
                drawLine(tint, Offset(size.width * .23f, size.height * .27f), Offset(size.width * .23f, size.height * .73f), stroke.width)
                drawLine(tint, Offset(size.width * .34f, size.height * .34f), Offset(size.width * .34f, size.height * .66f), stroke.width)
                drawLine(tint, Offset(size.width * .66f, size.height * .34f), Offset(size.width * .66f, size.height * .66f), stroke.width)
                drawLine(tint, Offset(size.width * .77f, size.height * .27f), Offset(size.width * .77f, size.height * .73f), stroke.width)
            }
            WallpadIconType.TABLE_LAMP -> {
                drawOval(
                    tint,
                    topLeft = Offset(size.width * .27f, size.height * .10f),
                    size = Size(size.width * .46f, size.height * .14f),
                    style = stroke,
                )
                drawLine(tint, Offset(size.width * .29f, size.height * .19f), Offset(size.width * .17f, size.height * .53f), stroke.width)
                drawLine(tint, Offset(size.width * .71f, size.height * .19f), Offset(size.width * .83f, size.height * .53f), stroke.width)
                drawArc(
                    tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * .16f, size.height * .43f),
                    size = Size(size.width * .68f, size.height * .20f),
                    style = stroke,
                )
                drawLine(tint, Offset(c.x, size.height * .58f), Offset(c.x, size.height * .80f), stroke.width)
                drawOval(
                    tint,
                    topLeft = Offset(size.width * .27f, size.height * .78f),
                    size = Size(size.width * .46f, size.height * .14f),
                    style = stroke,
                )
            }
            WallpadIconType.CEILING_LIGHT -> {
                val outer = Path().apply {
                    moveTo(size.width * .50f, size.height * .10f)
                    cubicTo(size.width * .53f, size.height * .10f, size.width * .57f, size.height * .13f, size.width * .61f, size.height * .15f)
                    lineTo(size.width * .87f, size.height * .36f)
                    cubicTo(size.width * .94f, size.height * .41f, size.width * .94f, size.height * .51f, size.width * .86f, size.height * .56f)
                    lineTo(size.width * .56f, size.height * .84f)
                    cubicTo(size.width * .52f, size.height * .88f, size.width * .48f, size.height * .88f, size.width * .44f, size.height * .84f)
                    lineTo(size.width * .14f, size.height * .56f)
                    cubicTo(size.width * .06f, size.height * .51f, size.width * .06f, size.height * .41f, size.width * .13f, size.height * .36f)
                    lineTo(size.width * .39f, size.height * .15f)
                    cubicTo(size.width * .43f, size.height * .13f, size.width * .47f, size.height * .10f, size.width * .50f, size.height * .10f)
                    close()
                }
                val inner = Path().apply {
                    moveTo(size.width * .50f, size.height * .19f)
                    lineTo(size.width * .82f, size.height * .40f)
                    cubicTo(size.width * .86f, size.height * .43f, size.width * .86f, size.height * .48f, size.width * .81f, size.height * .51f)
                    lineTo(size.width * .55f, size.height * .76f)
                    cubicTo(size.width * .52f, size.height * .79f, size.width * .48f, size.height * .79f, size.width * .45f, size.height * .76f)
                    lineTo(size.width * .19f, size.height * .51f)
                    cubicTo(size.width * .14f, size.height * .48f, size.width * .14f, size.height * .43f, size.width * .18f, size.height * .40f)
                    close()
                }
                drawPath(outer, tint, style = stroke)
                drawPath(inner, tint, style = stroke)
            }
            WallpadIconType.POWER_STRIP -> {
                drawRoundRect(
                    tint,
                    Offset(size.width * .27f, size.height * .08f),
                    Size(size.width * .46f, size.height * .84f),
                    CornerRadius(size.minDimension * .13f),
                    style = stroke,
                )
                listOf(.29f, .50f, .71f).forEach { y ->
                    drawCircle(tint, size.minDimension * .075f, Offset(c.x, size.height * y), style = stroke)
                    drawCircle(tint, size.minDimension * .018f, Offset(size.width * .46f, size.height * y),)
                    drawCircle(tint, size.minDimension * .018f, Offset(size.width * .54f, size.height * y),)
                }
            }
        }
    }
}
