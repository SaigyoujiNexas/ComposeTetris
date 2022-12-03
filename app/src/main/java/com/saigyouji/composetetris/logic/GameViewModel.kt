package com.saigyouji.composetetris.logic

import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

//MVI pattern,
class GameViewModel: ViewModel(){

    private val _viewState: MutableState<ViewState> = mutableStateOf(ViewState())
    val viewState: State<ViewState> = _viewState

    //dispatch the action.
    fun dispatch(action: Action) =
        reduce(viewState.value, action)

    //reduce the action dispatched and change the viewState to change the single data stream.
    private fun reduce(state: ViewState, action: Action){
        //directly call a coroutine, to avoid time-consuming operation.
        viewModelScope.launch {
            //all operation not needs IO. so use default dispatcher.
            withContext(Dispatchers.Default){
                //emit the new viewState to change the single direction data stream.
                emit(when(action){
                    Action.Reset -> run {
                        //if game is ending or not start, game start.
                        if(state.gameStatus == GameStatus.Onboard || state.gameStatus == GameStatus.GameOver)
                            return@run ViewState(
                                gameStatus = GameStatus.Running,
                                isMute = state.isMute
                            )
                        //else restart the game.
                        //clear the screen.
                        //after screen cleared, then set the state to onboard.
                        state.copy(gameStatus = GameStatus.ScreenClearing)
                            .also {
                                launch {
                                    clearScreen(state = state)
                                    emit(
                                        ViewState(
                                            gameStatus = GameStatus.Onboard,
                                            isMute = state.isMute
                                        )
                                    )
                                }
                            }
                    }
                    //if is running, set state to paused.
                    Action.Pause -> if(state.isRunning){
                        state.copy(gameStatus = GameStatus.Paused)
                    }else state
                    //if is paused. then resume the game.
                    Action.Resume -> if(state.isPaused){
                        state.copy(gameStatus = GameStatus.Running)
                    }else state
                    //if is move operation,
                    //offset the spirit.
                    is Action.Move -> run{
                        if(!state.isRunning) return@run state
                        SoundUtil.play(state.isMute, SoundType.Move)
                        val offset = action.direction.toOffset()
                        val spirit = state.spirit.moveBy(offset)
                        if(spirit.isValidInMatrix(state.bricks, state.matrix)){
                            state.copy(spirit = spirit)
                        }else{
                            state
                        }
                    }
                    //rotate.
                    //when you rotate the spirit in matrix boundary, they may be out of the matrix size range.
                    //so, adjust the spirit location, adjust it.
                    Action.Rotate -> run{
                        if(!state.isRunning) return@run state
                        //play the rotate sound.
                        SoundUtil.play(state.isMute, SoundType.Rotate)
                        val spirit = state.spirit.rotate().adjustOffSet(state.matrix)
                        if(spirit.isValidInMatrix(state.bricks, state.matrix)){
                            state.copy(spirit = spirit)
                        }else{
                            state
                        }
                    }
                    //drop to bottom.
                    Action.Drop -> run{
                        if(!state.isRunning) return@run state
                        SoundUtil.play(state.isMute,SoundType.Drop)
                        var i = 0
                        while(state.spirit.moveBy(0 to ++i)
                                .isValidInMatrix(state.bricks, state.matrix)){}
                        //out loop, the spirit is invalid, so move to previous location.
                        val spirit = state.spirit.moveBy(0 to  i - 1)
                        state.copy(spirit = spirit)
                    }

                    Action.GameTick -> run {
                        if (!state.isRunning) return@run state

                        if (state.spirit != Spirit.Empty) {
                            //drop.
                            val spirit = state.spirit.moveBy(Direction.Down.toOffset())
                            if (spirit.isValidInMatrix(state.bricks, state.matrix)) {
                                return@run state.copy(spirit = spirit)
                            }
                        }
                        //Game Over
                        if (!state.spirit.isValidInMatrix(state.bricks, state.matrix)) {
                            return@run state.copy(
                                gameStatus = GameStatus.ScreenClearing
                            ).also {
                                launch {
                                    emit(
                                        clearScreen(state = state).copy(gameStatus = GameStatus.GameOver)
                                    )
                                }
                            }
                        }
                        //Next Spirit
                        //解析语法。
                        val (updatedBricks, clearedLines) = updateBricks(
                            state.bricks,
                            state.spirit,
                            matrix = state.matrix
                        )

                        val (noClear, clearing, cleared) = updatedBricks
                        //get all type data.
                        val newState = state.copy(
                            spirit = state.spiritNext,
                            //get next spirit.
                            spiritReverse = (state.spiritReverse - state.spiritNext).takeIf { it.isNotEmpty() }
                                ?: generateSpiritReverse(state.matrix),
                            score = state.score + calculateScore(clearedLines) +
                                    if(state.spirit != Spirit.Empty) ScoreEverySpirit else 0,
                            line = state.line + clearedLines
                        )
                        //has line cleared.
                        if(clearedLines != 0){
                            SoundUtil.play(state.isMute, SoundType.Clean)
                            state.copy(gameStatus = GameStatus.LineClearing)
                                .also {
                                    launch {
                                        repeat(5) {
                                            emit(
                                                //进行一个闪烁
                                                state.copy(
                                                    gameStatus = GameStatus.LineClearing,
                                                    spirit = Spirit.Empty,
                                                    bricks = if(it % 2 == 0) noClear else clearing
                                                )
                                            )
                                            delay(100)
                                        }
                                        emit(
                                            newState.copy(
                                                bricks = cleared,
                                                gameStatus = GameStatus.Running
                                            )
                                        )
                                    }
                                }
                        }else{
                            //no line cleared.
                            newState.copy(bricks = noClear)
                        }
                    }

                    Action.Mute -> state.copy(isMute = !state.isMute)
                })
            }
        }
    }

    private suspend fun clearScreen(state: ViewState): ViewState{
        SoundUtil.play(state.isMute, SoundType.Start)

        val xRange = 0 until state.matrix.first
        var newState = state
        //from bottom top top.
        (state.matrix.second downTo 0).forEach { y ->
            emit(
                state.copy(
                    gameStatus =  GameStatus.ScreenClearing,
                    bricks = state.bricks + Brick.of(
                        xRange, y until state.matrix.second
                    )
                )
            )
            delay(50)
        }
        //from top to bottom.
        (0..state.matrix.second).forEach{y ->
            emit(
                state.copy(
                    gameStatus = GameStatus.ScreenClearing,
                    bricks = Brick.of(xRange, y until state.matrix.second)
                )
            )
            delay(50)
        }
        return newState
    }
    //change the single data stream.
    private fun emit(state: ViewState){
        _viewState.value = state
    }
    /**
     * Return a [Triple] to store clear-info for bricks.
     * -[Triple.first]: Bricks before clear line.
     * -[Triple.second]: Bricks after clear line but not offset.
     * -[Triple.third]: Bricks after clear.
     */
    private fun updateBricks(
        curBricks: List<Brick>,
        spirit: Spirit,
        matrix: Pair<Int, Int>
    ): Pair<Triple<List<Brick>, List<Brick>, List<Brick>>, Int>{
        val bricks = (curBricks + Brick.of(spirit))
        val map = mutableMapOf<Float, MutableSet<Float>>()
        bricks.forEach {
            map.getOrPut(it.location.y){
                mutableSetOf()
            }.add(it.location.x)
        }

        var clearing = bricks
        var cleared = bricks
        //sort by y position, and determine this line are full.
        val clearLines = map.entries.sortedBy { it.key }
            .filter {
                it.value.size == matrix.first
            }.map { it.key }
            .onEach { line ->
                //delete the full line.
                clearing = clearing.filter { it.location.y != line }
                //clear line and then offset brick.
                //for every y location smaller than full line's y value, plus one to move it down.
                cleared = cleared.filter{it.location.y != line}
                    .map { if(it.location.y < line) it.offsetBy(0 to 1) else it}
            }
        //return the final triple and cleared lines number.
        return Triple(bricks, clearing, cleared) to clearLines.size
    }

    //all view state collection.
    //single direction data stream.
    data class ViewState(
        val bricks: List<Brick> = emptyList(),
        val spirit: Spirit = Spirit.Empty,
        val spiritReverse: List<Spirit> = emptyList(),
        val matrix: Pair<Int, Int> = MatrixWidth to MatrixHeight,
        val gameStatus: GameStatus = GameStatus.Onboard,
        val score: Int = 0,
        val line: Int = 0,
        val isMute: Boolean = false,
    ){
        val level: Int
        get() = min(10, 1 + line / 20)

        val spiritNext: Spirit
        get() = spiritReverse.firstOrNull()?: Spirit.Empty

        val isPaused
        get() = gameStatus == GameStatus.Paused

        val isRunning
        get() = gameStatus == GameStatus.Running
    }
}
//control the data by this action.
//to change the data stream.
sealed interface Action{
    data class Move(val direction: Direction): Action
    object Reset: Action
    object  Pause: Action
    object Resume: Action
    object Rotate: Action
    object Drop: Action
    object GameTick: Action
    object Mute: Action
}

//game state collection.
enum class GameStatus{
    Onboard,
    Running,
    LineClearing,
    Paused,
    ScreenClearing,
    GameOver
}

private const val MatrixWidth = 12
private const val MatrixHeight = 24