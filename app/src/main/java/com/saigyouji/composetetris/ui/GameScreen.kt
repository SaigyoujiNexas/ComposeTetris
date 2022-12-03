package com.saigyouji.composetetris.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saigyouji.composetetris.R
import com.saigyouji.composetetris.logic.*
import com.saigyouji.composetetris.ui.theme.*
import kotlin.math.min

@Composable
fun GameScreen(modifier : Modifier = Modifier){
    val viewModel = viewModel<GameViewModel>()
    val viewState = viewModel.viewState.value

    Box(modifier = modifier
        .background(Color.Black)
        .padding(1.dp)
        .background(ScreenBackground)
        .padding(10.dp)){
        val animateValue by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500),
                repeatMode = RepeatMode.Reverse
            )
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ){
            val brickSize = min(
                size.width / viewState.matrix.first,
                size.height / viewState.matrix.second
            )
            drawMatrix(brickSize, viewState.matrix)
            drawMatrixBorder(brickSize, viewState.matrix)
            drawBricks(viewState.bricks, brickSize, viewState.matrix)
            drawSpirit(viewState.spirit, brickSize, viewState.matrix)
            drawText(viewState.gameStatus, brickSize, viewState.matrix, animateValue)
        }

        GameScoreboard(spirit = run {
            if(viewState.spirit == Spirit.Empty) Spirit.Empty
            else viewState.spiritNext.rotate()
        },
        score = viewState.score,
        line = viewState.line,
        level = viewState.level,
        isMute = viewState.isMute,
        isPaused = viewState.isPaused)
    }
}
@Composable
fun GameScoreboard(
    modifier: Modifier = Modifier,
    brickSize: Float = 35f,
    spirit: Spirit,
    score: Int = 0,
    line : Int = 0,
    level: Int = 1,
    isMute: Boolean = false,
    isPaused: Boolean = false
){
    Row(modifier.fillMaxSize()){
        Spacer(modifier = Modifier.weight(0.65f))
        val textSize = 12.sp
        val margin = 12.dp
        Column(
            Modifier
                .fillMaxHeight()
                .weight(0.35f)
        ){
            Text("Score", fontSize = textSize)
            LedNumber(Modifier.fillMaxWidth(), score, 6)

            Spacer(modifier = Modifier.height(margin))

            Text("Lines", fontSize = textSize)
            LedNumber(modifier.fillMaxWidth(), line, 6)

            Spacer(modifier = Modifier.height(margin))

            Text("Level", fontSize = textSize)
            LedNumber(modifier = Modifier.fillMaxWidth(), level, 1)

            Spacer(modifier = Modifier.height(margin))

            Text("Next", fontSize = textSize)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(10.dp)
            ){
                drawMatrix(brickSize, NextMatrix)
                drawSpirit(
                    spirit.adjustOffSet(NextMatrix),
                    brickSize = brickSize, NextMatrix
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Row{
                Image(
                    modifier = Modifier.width(15.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_music_off_24),
                    colorFilter =  ColorFilter.tint(if(isMute) BrickSpirit else BrickMatrix),
                    contentDescription = null
                )
                Image(
                    modifier = Modifier.width(16.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_pause_24),
                    colorFilter =  ColorFilter.tint(if(isPaused) BrickSpirit else BrickMatrix),
                    contentDescription = null)
                Spacer(modifier = Modifier.weight(1f))

                LedClock()
            }
        }
    }
}
private fun DrawScope.drawText(
    gameStatus: GameStatus,
    brickSize: Float,
    matrix: Pair<Int, Int>,
    alpha: Float
){
    val center = androidx.compose.ui.geometry.Offset(
        brickSize * matrix.first / 2,
        brickSize * matrix.second / 2
    )
    val drawText  = {text: String, size: Float ->
        drawIntoCanvas {
            it.nativeCanvas.drawText(
                text,
                center.x,
                center.y,
                android.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = alpha).toArgb()
                    textSize = size
                    textAlign = android.graphics.Paint.Align.CENTER
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                    strokeWidth = size / 12
                }
            )
        }
    }
    if(gameStatus == GameStatus.Onboard){
        drawText("TETRIS", 80f)
    }else if (gameStatus == GameStatus.GameOver){
        drawText("GAME OVER", 60f)
    }
}

private fun DrawScope.drawMatrix(brickSize: Float, matrix: Pair<Int, Int>){
    (0 until matrix.first).forEach { x ->
        (0 until matrix.second).forEach { y ->
            drawBrick(
                brickSize,
                Offset(x.toFloat(), y.toFloat()),
                BrickMatrix
            )
        }
    }
}
private fun DrawScope.drawMatrixBorder(brickSize: Float, matrix: Pair<Int, Int>){
    val gap = matrix.first * brickSize * 0.05f
    drawRect(color = Color.Black,
    size = Size(
        matrix.first * brickSize + gap,
        matrix.second * brickSize + gap
    ),
        topLeft = androidx.compose.ui.geometry.Offset(-gap / 2, -gap / 2),
        style = Stroke(1.dp.toPx())
    )
}

private fun DrawScope.drawBricks(brick: List<Brick>, brickSize: Float, matrix: Pair<Int, Int>){
    clipRect(0f, 0f,
    matrix.first * brickSize,
    matrix.second * brickSize){
        brick.forEach{
            drawBrick(brickSize, it.location, BrickSpirit)
        }
    }
}
private fun DrawScope.drawSpirit(spirit: Spirit, brickSize: Float, matrix: Pair<Int, Int>){
    clipRect(0f, 0f,
    matrix.first * brickSize,
    matrix.second * brickSize){
        spirit.location.forEach{
            drawBrick(brickSize,
            Offset(it.x, it.y),
            BrickSpirit)
        }
    }
}
private fun DrawScope.drawBrick(
    brickSize: Float,
    offset: Offset,
    color: Color
){
    val actualLocation = Offset(
        offset.x * brickSize,
        offset.y * brickSize
    )
    val outerSize = brickSize * 0.8f

    val outerOffset = (brickSize - outerSize) / 2

    drawRect(
        color = color,
        topLeft = actualLocation + Offset(outerOffset, outerOffset),
        size = Size(outerSize, outerSize),
        style = Stroke(outerSize / 10)
    )
    val innerSize = brickSize * 0.5f
    val innerOffset = (brickSize - innerSize) / 2

    drawRect(color,
    actualLocation + Offset(innerOffset, innerOffset),
    size = Size(innerSize, innerSize))
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewGameScreen(){
    GameScreen()
}
