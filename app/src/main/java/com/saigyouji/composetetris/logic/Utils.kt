package com.saigyouji.composetetris.logic

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.saigyouji.composetetris.R


fun Offset(x: Int, y: Int) = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())

//control direction define.
enum class Direction{
    Left, Up, Right, Down
}

//map direction to offset.
fun Direction.toOffset() = when(this){
    Direction.Left -> -1 to 0
    Direction.Up-> 0 to -1
    Direction.Right-> 1 to 0
    Direction.Down -> 0 to 1
}

//font set.
val LedFontFamily = FontFamily(
    Font(R.font.unidream_led, FontWeight.Light),
    Font(R.font.unidream_led, FontWeight.Normal),
    Font(R.font.unidream_led, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.unidream_led, FontWeight.Medium),
    Font(R.font.unidream_led, FontWeight.Bold)
)
//next matrix size.
val NextMatrix = 4 to 2


const val ScoreEverySpirit = 12

fun calculateScore(lines: Int) = when(lines){
    1 -> 100
    2 -> 300
    3 -> 700
    4 -> 1500
    else -> 0
}

object StatusBarUtil{
    fun transparentStatusBar(activity: Activity){
        with(activity){
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            val option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            val vis = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = option or vis
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}

@SuppressLint("StaticFieldLeak")
object SoundUtil{
    private var _context: Context? = null
    private val sp: SoundPool by lazy {
        SoundPool.Builder().setMaxStreams(4).setMaxStreams(AudioManager.STREAM_MUSIC).build()
    }

    private val _map = mutableMapOf<SoundType, Int>()

    fun init(context: Context){
        _context = context
        Sounds.forEach {
            _map[it] = sp.load(_context, it.res, 1)
        }
    }

    fun release(){
        _context = null
        sp.release()
    }

    fun play(isMute: Boolean, sound: SoundType){
        if(!isMute){
            sp.play(requireNotNull(_map[sound]), 1f, 1f, 0, 0, 1f)
        }
    }
}
sealed class SoundType(val res: Int){
    object Move: SoundType(R.raw.move)
    object Rotate: SoundType(R.raw.rotate)
    object Start: SoundType(R.raw.start)
    object Drop: SoundType(R.raw.drop)
    object Clean : SoundType(R.raw.clean)
}

val Sounds =
    listOf(SoundType.Move, SoundType.Rotate, SoundType.Start, SoundType.Drop, SoundType.Clean)