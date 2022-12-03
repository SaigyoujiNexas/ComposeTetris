package com.saigyouji.composetetris.logic

import androidx.compose.ui.geometry.Offset
import kotlin.math.absoluteValue
import kotlin.random.Random

/*
对基本块的数据类定义
    shape: The Shape that defined by offset.
    offset: Spirit 的偏移量。
 */
data class Spirit(
    val shape: List<Offset> = emptyList(),
    val offset: Offset = Offset(0f, 0f),
){
    //根据shape和偏移量计算每一个像素块的位置。
    val location: List<Offset> = shape.map { it + offset }

    //移动
    fun moveBy(step: Pair<Int, Int>): Spirit =
        copy(offset = offset + Offset(step.first, step.second))

    //旋转
    fun rotate(): Spirit{
        val newShape = shape.toMutableList()
        //对于顺时针旋转，每一个点作如下变换:
        //新的y值为负的旧的x值， 新的x值为旧的y值。
        //sin(x - pi / 2) = -cos(x)
        //cos(x - pi / 2) = sin(x)
        for(i in shape.indices){
            newShape[i] = Offset(shape[i].y, -shape[i].x)
        }
        return copy(shape = newShape)
    }
    /*
    * 在矩阵中调整整体的偏移值。
    * @param matrix: 矩阵大小
     */
    fun adjustOffSet(matrix: Pair<Int, Int>, adjustY: Boolean = true): Spirit{
        //计算y轴的偏移值。
        //first, if this spirit have min y value, then get this y value.
        //second, if this min y value smaller than zero, then the first calculate value is absolute value, else is zero.
        //third, get the max y value, if the y value bigger than matrix height, then set the value to height reduce min y value - 1.
        //but if first and second calculate process is not zero, then the third process mast be zero.
        //so, if y offset smaller than zero, then move it to zero, also is moveBy(abs(minYvalue)).
        //if y offset bigger than height, define is
        val yOffset =
            if(adjustY)
                (location.minByOrNull { it.y }?.y?.takeIf { it < 0 }?.absoluteValue?: 0).toInt() +
                        (location.maxByOrNull { it.y }?.y?.takeIf { it > matrix.second - 1 }
                            ?.let { matrix.second - it - 1 } ?: 0).toInt()
            else 0
        //same way, adjust the x value, set it to default position.
        val xOffset =
            (location.minByOrNull { it.x }?.x?.takeIf { it < 0 }?.absoluteValue ?: 0).toInt() +
                    (location.maxByOrNull { it.x }?.x?.takeIf{it > matrix.first - 1}
                        ?.let { matrix.first - it - 1 } ?: 0).toInt()
        return moveBy(xOffset to yOffset)
    }
    companion object{
        //create a empty spirit.
        val Empty = Spirit()
    }
}

//define the all type spirits.
val SpiritType = listOf(
    listOf(Offset(1, -1),Offset(1, 0), Offset(0, 0), Offset(0, 1)),//Z
    listOf(Offset(0, -1), Offset(0, 0), Offset(1, 0), Offset(1, 1)),//S
    listOf(Offset(0, -1), Offset(0, 0), Offset(0, 1), Offset(0, 2)),//I
    listOf(Offset(0, 1), Offset(0, 0), Offset(0, -1), Offset(1, 0)),//T
    listOf(Offset(1, 0), Offset(0, 0), Offset(1, -1), Offset(0, -1)),//O
    listOf(Offset(0, -1), Offset(1, -1), Offset(1, 0), Offset(1, 1)),//L
    listOf(Offset(1, -1), Offset(0, -1), Offset(0, 0), Offset(0, 1))//J
)
//determine the spirit is valid.
fun Spirit.isValidInMatrix(block: List<Brick>, matrix: Pair<Int, Int>): Boolean{
    //valid spirit all location must in this matrix.
    //and must not in block collection.
    return location.none{location ->
        location.x < 0 || location.x > matrix.first - 1 ||
                location.y > matrix.second - 1 ||
                block.any { it.location.x == location.x && it.location.y == location.y }
    }
}
/*
generate the spirit
 */
fun generateSpiritReverse(matrix: Pair<Int, Int>): List<Spirit>{
    return SpiritType.map {
        Spirit(it, Offset(Random.nextInt(matrix.first - 1), -1)).adjustOffSet(matrix, false)
    }
}