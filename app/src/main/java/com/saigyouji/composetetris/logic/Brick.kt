package com.saigyouji.composetetris.logic

import androidx.compose.ui.geometry.Offset

//最基本的像素块定义数据类。
data class Brick(val location: Offset = Offset(0f, 0f)){
    companion object{
        /*
        将偏移数组转换为Brick。
         */
        fun of(offsetList: List<Offset>) = offsetList.map { Brick(it)}

        fun of (spirit: Spirit) = of(spirit.location)

        fun of(xRange: IntRange, yRange: IntRange) =
            of(mutableListOf<Offset>().apply {
                xRange.forEach{x ->
                    yRange.forEach{y ->
                        this += Offset(x, y)
                    }
                }
            })
    }
    fun offsetBy(step: Pair<Int, Int>) =
        copy(location = Offset(location.x + step.first, location.y + step.second))
}