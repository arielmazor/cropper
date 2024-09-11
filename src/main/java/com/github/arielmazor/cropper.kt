package com.github.arielmazor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

enum class Handle {
    Top,
    Bottom
}

@Composable
fun rememberCropperState(image: ImageBitmap): CropperState {
    return remember { CropperState(image) }
}

class CropperState(
    val image: ImageBitmap,
) {
    var height: Float by mutableFloatStateOf(0f)
    private lateinit var density: Density
    var canvasWidth = 0f
    private var initialOffsetX = 0f
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    val scale = Animatable(1f)
    var activeHandles: Handle? by mutableStateOf(null)
    val handleOffset = Animatable(0f)

    suspend fun init(maxWidth: Float, maxHeight: Float, density: Density) {
        this.canvasWidth = maxWidth * .85f
        this.height = maxHeight
        this.density = density
        this.offsetX = density.run { maxWidth / 2 - canvasWidth / 2 }
        this.offsetY =
            density.run { (maxHeight / 2 - (image.height * canvasWidth / image.width).toInt() / 2) }
        this.scale.snapTo(1f)
        this.initialOffsetX = this.offsetX
    }

    fun crop(): ImageBitmap {
        val imageBitmap = ImageBitmap(canvasWidth.toInt(), canvasWidth.toInt())

        val canvas = Canvas(imageBitmap)

        canvas.translate(0f, -(height / 2) + canvasWidth / 2)

        canvas.clipPath(Path().apply {
            addOval(
                Rect(
                    Offset(canvasWidth / 2f, height / 2f),
                    canvasWidth / 2f
                )
            )
        })

        canvas.drawRect(
            Rect(Offset(canvasWidth / 2f, height / 2f), canvasWidth / 2f), Paint()
        )

        canvas.scale(
            scale.value,
            scale.value,
            (canvasWidth / 2),
            (height / 2)
        )

        canvas.translate(offsetX - initialOffsetX, offsetY)

        canvas.drawImageRect(
            image,
            dstSize = IntSize(
                canvasWidth.toInt(),
                (image.height * canvasWidth / image.width).toInt()
            ),
            paint = Paint()
        )


        return imageBitmap
    }
}


@Composable
fun Cropper(state: CropperState) {
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        LaunchedEffect(key1 = null) {
            density.run { state.init(maxWidth.toPx(), maxHeight.toPx(), this) }
        }

        CropperImpl(state)
    }
}

@Composable
private fun CropperImpl(state: CropperState) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val transformState = rememberTransformableState { zoom, pan, _ ->
        scope.launch {
            awaitAll(
                async {
                    state.offsetX += pan.x
                    state.offsetY += pan.y
                },
                async {
                    if (state.scale.value != state.scale.value * zoom) {
                        state.scale.snapTo((state.scale.value * zoom).coerceIn(.5f, 4f))
                    }
                }
            )
        }
    }

    val gridAlpha by animateFloatAsState(
        targetValue = if (transformState.isTransformInProgress || state.activeHandles != null) 1f else 0f,
        label = ""
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xff141215))
            .statusBarsPadding()
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(density.run { state.height.toDp() })
                    .transformable(transformState)
                    .clipToBounds(),
            ) {
                scale(state.scale.value) {
                    drawImage(
                        state.image,
                        dstOffset = IntOffset(state.offsetX.toInt(), state.offsetY.toInt()),
                        dstSize = IntSize(
                            state.canvasWidth.toInt(),
                            (state.image.height * state.canvasWidth / state.image.width).toInt()
                        )
                    )
                }

                withTransform({
                    clipPath(Path().apply {
                        addOval(
                            Rect(
                                center = center,
                                radius = (density.run { state.canvasWidth } - state.handleOffset.value) / 2
                            )
                        )
                    }, ClipOp.Difference)
                }) {
                    drawRect(Color(0xff141215).copy(alpha = .7f))
                }
            }

            Box(modifier = Modifier
                .align(Alignment.Center)
                .pointerInput(null) {
                    detectTapGestures(
                        onPress = {
                            state.activeHandles = if (it.y < state.canvasWidth / 2) {
                                Handle.Top
                            } else {
                                Handle.Bottom
                            }
                        },
                        onTap = { state.activeHandles = null })
                }
                .pointerInput(null) {
                    detectDragGestures(
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                if (state.activeHandles != null) {
                                    val newDrag =
                                        dragAmount.y * if (state.activeHandles == Handle.Bottom) -1 else 1
                                    if (state.scale.value < 3 && state.handleOffset.value + newDrag in 0f..(size.width * .5f)) {
                                        state.handleOffset.snapTo(state.handleOffset.value + newDrag)
                                    } else if (newDrag < 0) {
                                        state.scale.snapTo(
                                            (state.scale.value - 0.05f).coerceAtLeast(
                                                .5f
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            density.run {
                                scope.launch {
                                    val newScale =
                                        (state.canvasWidth / ((state.canvasWidth - (state.handleOffset.value) * 2) / state.scale.value))
                                    awaitAll(
                                        async {
                                            state.activeHandles = null
                                        },
                                        async {
                                            state.scale.animateTo(newScale.coerceAtMost(4f))
                                        },
                                        async {
                                            state.handleOffset.animateTo(0f)
                                        }
                                    )

                                }
                            }
                        }
                    )
                }
                .padding(20.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(density.run { state.canvasWidth.toDp() - state.handleOffset.value.toDp() })
                ) {
                    withTransform({
                        clipRect()
                    }) {
                        // top
                        drawRect(
                            color = Color(0xff99a1a9),
                            size = Size(size.width, 2.dp.toPx()),
                        )

                        // bottom

                        drawRect(
                            color = Color(0xff99a1a9),
                            size = Size(size.width, 2.dp.toPx()),
                            topLeft = Offset(
                                x = 0f,
                                y = size.height - 2.dp.toPx()
                            )
                        )

                        // left

                        drawRect(
                            color = Color(0xff99a1a9),
                            size = Size(2.dp.toPx(), size.height),
                        )

                        // right

                        drawRect(
                            color = Color(0xff99a1a9),
                            size = Size(2.dp.toPx(), size.height),
                            topLeft = Offset(
                                x = size.width - 2.dp.toPx(),
                                y = 0f
                            )
                        )

                    }

                    // top left

                    drawRect(
                        color = Color.White,
                        size = Size(30.dp.toPx(), 5.dp.toPx()),
                        topLeft = Offset(
                            x = -3.dp.toPx(),
                            y = -3.dp.toPx()
                        )
                    )

                    drawRect(
                        color = Color.White,
                        size = Size(5.dp.toPx(), 30.dp.toPx()),
                        topLeft = Offset(
                            x = -3.dp.toPx(),
                            y = -3.dp.toPx()
                        )
                    )

                    // top right

                    drawRect(
                        color = Color.White,
                        size = Size(30.dp.toPx(), 5.dp.toPx()),
                        topLeft = Offset(
                            x = size.width - 30.dp.toPx(),
                            y = -3.dp.toPx()
                        )
                    )

                    drawRect(
                        color = Color.White,
                        size = Size(5.dp.toPx(), 30.dp.toPx()),
                        topLeft = Offset(
                            x = size.width - 2.dp.toPx(),
                            y = -3.dp.toPx()
                        )
                    )

                    // bottom left

                    drawRect(
                        color = Color.White,
                        size = Size(30.dp.toPx(), 5.dp.toPx()),
                        topLeft = Offset(
                            x = -3.dp.toPx(),
                            y = size.height - 2.dp.toPx()
                        )
                    )

                    drawRect(
                        color = Color.White,
                        size = Size(5.dp.toPx(), 30.dp.toPx()),
                        topLeft = Offset(
                            x = -3.dp.toPx(),
                            y = size.height - 30.dp.toPx()
                        )
                    )

                    // bottom right

                    drawRect(
                        color = Color.White,
                        size = Size(30.dp.toPx(), 5.dp.toPx()),
                        topLeft = Offset(
                            x = size.width - 27.dp.toPx(),
                            y = size.height - 2.dp.toPx()
                        )
                    )

                    drawRect(
                        color = Color.White,
                        size = Size(5.dp.toPx(), 30.dp.toPx()),
                        topLeft = Offset(
                            x = size.width - 2.dp.toPx(),
                            y = size.height - 27.dp.toPx()
                        )
                    )

                    // grid

                    drawRect(
                        color = Color(0xff99a1a9).copy(alpha = gridAlpha),
                        size = Size(2.dp.toPx(), size.height),
                        topLeft = Offset(x = size.width / 3, 0f)
                    )

                    drawRect(
                        color = Color(0xff99a1a9).copy(alpha = gridAlpha),
                        size = Size(2.dp.toPx(), size.height),
                        topLeft = Offset(x = (size.width / 3) * 2, 0f)
                    )

                    drawRect(
                        color = Color(0xff99a1a9).copy(alpha = gridAlpha),
                        size = Size(size.width, 2.dp.toPx()),
                        topLeft = Offset(x = 0f, size.height / 3)
                    )

                    drawRect(
                        color = Color(0xff99a1a9).copy(alpha = gridAlpha),
                        size = Size(size.width, 2.dp.toPx()),
                        topLeft = Offset(0f, (size.height / 3) * 2)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(density.run { state.canvasWidth.toDp() })
                    .clip(CircleShape)
                    .transformable(transformState)
            )
        }
    }
}