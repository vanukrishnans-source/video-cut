package com.vanu.videocut.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vanu.videocut.model.CropRect
import com.vanu.videocut.ui.theme.Amber
import kotlin.math.max
import kotlin.math.min

private enum class CropHandle { MOVE, L, R, T, B, TL, TR, BL, BR }

@Composable
fun CropOverlay(
    crop: CropRect,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    var handle by remember { mutableStateOf<CropHandle?>(null) }
    val stroke = 3.dp

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(crop) {
                val hit = 48.dp.toPx()
                detectDragGestures(
                    onDragStart = { pos ->
                        val rect = Rect(
                            crop.left * size.width,
                            crop.top * size.height,
                            crop.right * size.width,
                            crop.bottom * size.height,
                        )
                        handle = when {
                            near(pos, Offset(rect.left, rect.top), hit) -> CropHandle.TL
                            near(pos, Offset(rect.right, rect.top), hit) -> CropHandle.TR
                            near(pos, Offset(rect.left, rect.bottom), hit) -> CropHandle.BL
                            near(pos, Offset(rect.right, rect.bottom), hit) -> CropHandle.BR
                            absEdge(pos.x, rect.left, hit) && pos.y in rect.top..rect.bottom -> CropHandle.L
                            absEdge(pos.x, rect.right, hit) && pos.y in rect.top..rect.bottom -> CropHandle.R
                            absEdge(pos.y, rect.top, hit) && pos.x in rect.left..rect.right -> CropHandle.T
                            absEdge(pos.y, rect.bottom, hit) && pos.x in rect.left..rect.right -> CropHandle.B
                            pos.x in rect.left..rect.right && pos.y in rect.top..rect.bottom -> CropHandle.MOVE
                            else -> null
                        }
                    },
                    onDragEnd = { handle = null },
                    onDragCancel = { handle = null },
                    onDrag = { change, drag ->
                        change.consume()
                        val h = handle ?: return@detectDragGestures
                        val dx = drag.x / size.width
                        val dy = drag.y / size.height
                        var l = crop.left
                        var t = crop.top
                        var r = crop.right
                        var b = crop.bottom
                        when (h) {
                            CropHandle.MOVE -> {
                                val w = r - l
                                val hh = b - t
                                l = (l + dx).coerceIn(0f, 1f - w)
                                t = (t + dy).coerceIn(0f, 1f - hh)
                                r = l + w
                                b = t + hh
                            }
                            CropHandle.L -> l = min(l + dx, r - 0.05f).coerceAtLeast(0f)
                            CropHandle.R -> r = max(r + dx, l + 0.05f).coerceAtMost(1f)
                            CropHandle.T -> t = min(t + dy, b - 0.05f).coerceAtLeast(0f)
                            CropHandle.B -> b = max(b + dy, t + 0.05f).coerceAtMost(1f)
                            CropHandle.TL -> {
                                l = min(l + dx, r - 0.05f).coerceAtLeast(0f)
                                t = min(t + dy, b - 0.05f).coerceAtLeast(0f)
                            }
                            CropHandle.TR -> {
                                r = max(r + dx, l + 0.05f).coerceAtMost(1f)
                                t = min(t + dy, b - 0.05f).coerceAtLeast(0f)
                            }
                            CropHandle.BL -> {
                                l = min(l + dx, r - 0.05f).coerceAtLeast(0f)
                                b = max(b + dy, t + 0.05f).coerceAtMost(1f)
                            }
                            CropHandle.BR -> {
                                r = max(r + dx, l + 0.05f).coerceAtMost(1f)
                                b = max(b + dy, t + 0.05f).coerceAtMost(1f)
                            }
                        }
                        onCropChange(CropRect(l, t, r, b))
                    },
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val left = crop.left * w
        val top = crop.top * h
        val right = crop.right * w
        val bottom = crop.bottom * h

        // Dim outside
        val dim = Color(0x99000000)
        drawRect(dim, Offset(0f, 0f), Size(w, top))
        drawRect(dim, Offset(0f, bottom), Size(w, h - bottom))
        drawRect(dim, Offset(0f, top), Size(left, bottom - top))
        drawRect(dim, Offset(right, top), Size(w - right, bottom - top))

        // Border
        drawRect(
            color = Amber,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = stroke.toPx()),
        )

        // Corner handles
        val hs = 18.dp.toPx()
        val corners = listOf(
            Offset(left, top), Offset(right, top),
            Offset(left, bottom), Offset(right, bottom),
        )
        corners.forEach { c ->
            drawCircle(Amber, radius = hs / 2f, center = c)
            drawCircle(Color.White, radius = hs / 4f, center = c)
        }
    }
}

private fun near(a: Offset, b: Offset, hit: Float): Boolean {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy <= hit * hit
}

private fun absEdge(v: Float, edge: Float, hit: Float): Boolean =
    kotlin.math.abs(v - edge) <= hit
