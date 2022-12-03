package com.saigyouji.composetetris.ui.theme

import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saigyouji.composetetris.logic.LedFontFamily
import kotlinx.coroutines.CoroutineStart
import java.util.*
import kotlin.math.roundToInt

@Composable
fun LedClock(modifier: Modifier = Modifier){
    val animateValue by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    var clock by remember{ mutableStateOf(0 to 0)}

    DisposableEffect(key1 = animateValue.roundToInt()){
        val dateFormat: DateFormat = SimpleDateFormat("H,m")
        val(curHou, curMin) = dateFormat.format(Date()).split(",")
        clock = curHou.toInt() to curMin.toInt()
        onDispose {  }
    }
    Row(modifier){
        LedNumber(num = clock.first, digits = 2, fillZero = true)

        val LedComma: @Composable (color: Color) -> Unit = remember {{
            Text(
                text = ":",
                fontFamily = LedFontFamily,
                textAlign = TextAlign.End,
                color = it,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }}

        Box(modifier = Modifier
            .width(6.dp)
            .padding(end = 1.dp)){
            LedComma(BrickMatrix)
            //闪烁特效
            if(animateValue.roundToInt() == 1){
                LedComma(BrickSpirit)
            }
        }
        LedNumber(num = clock.second, digits = 2, fillZero = true)
    }
}

@Composable
fun LedNumber(
    modifier: Modifier = Modifier,
    num: Int,
    digits: Int,
    fillZero: Boolean = false
){
    val textSize = 16.sp
    val textWidth = 8.dp
    Box(modifier){
        //Shadow.
        Row(modifier = Modifier.align(Alignment.CenterEnd)){
            repeat(digits){
                Text(
                    text = "8",
                    fontSize = textSize,
                    color = BrickMatrix,
                    fontFamily = LedFontFamily,
                    modifier = Modifier.width(textWidth),
                    textAlign = TextAlign.End
                )
            }
        }
        //Real.
        Row(
            modifier = Modifier.align(Alignment.CenterEnd)
        ){
            val str = if(fillZero) String.format("%0${digits}d",num) else num.toString()
            str.iterator().forEach {
                Text(
                    text = it.toString(),
                    fontSize = textSize,
                    color = BrickSpirit,
                    fontFamily = LedFontFamily,
                    modifier = Modifier.width(textWidth),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
@Preview
fun PreviewLedClock(){
    LedClock()
}