package com.example.my_tick_tac_toe

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.my_tick_tac_toe.databinding.GameScreenBinding

class GameSession : AppCompatActivity() {
    private lateinit var ui: GameScreenBinding
    private lateinit var board: Array<Array<String>>
    private lateinit var bgMusic: MediaPlayer
    private var gameActive = true
    private var gameRestored = false

    private val settingsResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            setupVolume()
            ui.gameTimer.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = GameScreenBinding.inflate(layoutInflater)

        setupBoard()
        setupControls()
        setupAudio()

        setContentView(ui.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        bgMusic.release()
    }

    private fun setupBoard() {
        val savedTime = intent.getLongExtra(SAVED_GAME_TIME, 0L)
        val savedState = intent.getStringExtra(SAVED_GAME_STATE)

        if (savedState != null && savedTime != 0L && savedState.isNotEmpty()) {
            restoreGame(savedTime, savedState)
            gameRestored = true
        } else {
            initializeBoard()
        }
    }

    private fun setupControls() {
        ui.exitButton.setOnClickListener { finish() }
        ui.menuButton.setOnClickListener { showGameMenu() }

        val cells = listOf(
            ui.cell1 to (0 to 0), ui.cell2 to (0 to 1), ui.cell3 to (0 to 2),
            ui.cell4 to (1 to 0), ui.cell5 to (1 to 1), ui.cell6 to (1 to 2),
            ui.cell7 to (2 to 0), ui.cell8 to (2 to 1), ui.cell9 to (2 to 2)
        )

        cells.forEach { (view, pos) ->
            view.setOnClickListener {
                if (gameActive) makePlayerMove(pos.first, pos.second)
            }
        }
    }

    private fun setupAudio() {
        bgMusic = MediaPlayer.create(this, R.raw.game_music).apply {
            isLooping = true
        }
        setupVolume()
        bgMusic.start()
        ui.gameTimer.base = SystemClock.elapsedRealtime()
        ui.gameTimer.start()
    }

    private fun setupVolume()
    {
        val settings = loadSettings()
        bgMusic.setVolume(settings.volume / 100f, settings.volume / 100f)
    }

    private fun restoreGame(time: Long, state: String) {
        ui.gameTimer.base = SystemClock.elapsedRealtime() - time
        board = state.split("\n").map { it.split(";").toTypedArray() }.toTypedArray()
        updateBoardUI()
    }

    private fun initializeBoard() {
        board = Array(3) { Array(3) { EMPTY } }
    }

    private fun makePlayerMove(row: Int, col: Int) {
        if (board[row][col] != EMPTY) {
            Toast.makeText(this, "Cell occupied", Toast.LENGTH_SHORT).show()
            return
        }

        board[row][col] = PLAYER
        updateCellUI(row, col, PLAYER)

        when (checkGameResult(row, col, PLAYER)) {
            GameResult.PLAYER_WIN -> showResultDialog(GameResult.PLAYER_WIN)
            GameResult.DRAW -> showResultDialog(GameResult.DRAW)
            else -> makeAIMove()
        }
    }

    private fun makeAIMove() {
        val settings = loadSettings()
        val move = when (settings.difficulty) {
            0 -> makeRandomMove()
            1 -> makeSmartMove()
            else -> makeOptimalMove()
        }

        board[move.row][move.col] = AI
        updateCellUI(move.row, move.col, AI)

        when (checkGameResult(move.row, move.col, AI)) {
            GameResult.AI_WIN -> showResultDialog(GameResult.AI_WIN)
            GameResult.DRAW -> showResultDialog(GameResult.DRAW)
            else -> {}
        }
    }

    private fun makeRandomMove(): Move {
        val emptyCells = mutableListOf<Move>()
        board.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, cell ->
                if (cell == EMPTY) emptyCells.add(Move(row, col))
            }
        }
        return emptyCells.random()
    }

    private fun makeSmartMove(): Move {
        // Проверка если AI может выиграть
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == EMPTY) {
                    board[row][col] = AI
                    if (checkGameResult(row, col, AI) == GameResult.AI_WIN) {
                        board[row][col] = EMPTY
                        return Move(row, col)
                    }
                    board[row][col] = EMPTY
                }
            }
        }

        // Блокировать игрока если он может выиграть
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == EMPTY) {
                    board[row][col] = PLAYER
                    if (checkGameResult(row, col, PLAYER) == GameResult.PLAYER_WIN) {
                        board[row][col] = EMPTY
                        return Move(row, col)
                    }
                    board[row][col] = EMPTY
                }
            }
        }

        // Иначе рандомный ход
        return makeRandomMove()
    }

    private fun makeOptimalMove(): Move {
        var bestScore = Int.MIN_VALUE
        var bestMove = Move(0, 0)

        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == EMPTY) {
                    board[row][col] = AI
                    val score = minimax(false)
                    board[row][col] = EMPTY
                    if (score > bestScore) {
                        bestScore = score
                        bestMove = Move(row, col)
                    }
                }
            }
        }
        return bestMove
    }

    private fun minimax(isMaximizing: Boolean): Int {
        val result = getCurrentGameState()
        return when {
            result == GameResult.AI_WIN -> 10
            result == GameResult.PLAYER_WIN -> -10
            result == GameResult.DRAW -> 0
            isMaximizing -> {
                var maxScore = Int.MIN_VALUE
                for (row in 0..2) {
                    for (col in 0..2) {
                        if (board[row][col] == EMPTY) {
                            board[row][col] = AI
                            val score = minimax(false)
                            board[row][col] = EMPTY
                            maxScore = maxOf(maxScore, score)
                        }
                    }
                }
                maxScore
            }
            else -> {
                var minScore = Int.MAX_VALUE
                for (row in 0..2) {
                    for (col in 0..2) {
                        if (board[row][col] == EMPTY) {
                            board[row][col] = PLAYER
                            val score = minimax(true)
                            board[row][col] = EMPTY
                            minScore = minOf(minScore, score)
                        }
                    }
                }
                minScore
            }
        }
    }

    private fun checkGameResult(row: Int, col: Int, player: String): GameResult {
        val settings = loadSettings()
        val winConditions = settings.winConditions

        fun checkLine(line: List<String>): Boolean = line.all { it == player }

        val rowCheck = winConditions and 1 != 0 && checkLine(board[row].toList())
        val colCheck = winConditions and 2 != 0 && checkLine(board.map { it[col] })
        val diag1Check = winConditions and 4 != 0 && row == col && checkLine((0..2).map { board[it][it] })
        val diag2Check = winConditions and 4 != 0 && row + col == 2 && checkLine((0..2).map { board[it][2-it] })

        return when {
            rowCheck || colCheck || diag1Check || diag2Check -> {
                if (player == PLAYER) GameResult.PLAYER_WIN else GameResult.AI_WIN
            }
            isBoardFull() -> GameResult.DRAW
            else -> GameResult.ONGOING
        }
    }

    private fun getCurrentGameState(): GameResult {
        // Проверяем все возможные условия выигрыша для обоих игроков
        for (row in 0..2) {
            if (board[row][0] != EMPTY && board[row][0] == board[row][1] && board[row][1] == board[row][2]) {
                return if (board[row][0] == PLAYER) GameResult.PLAYER_WIN else GameResult.AI_WIN
            }
        }

        for (col in 0..2) {
            if (board[0][col] != EMPTY && board[0][col] == board[1][col] && board[1][col] == board[2][col]) {
                return if (board[0][col] == PLAYER) GameResult.PLAYER_WIN else GameResult.AI_WIN
            }
        }

        if (board[0][0] != EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return if (board[0][0] == PLAYER) GameResult.PLAYER_WIN else GameResult.AI_WIN
        }

        if (board[0][2] != EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return if (board[0][2] == PLAYER) GameResult.PLAYER_WIN else GameResult.AI_WIN
        }

        return if (isBoardFull()) GameResult.DRAW else GameResult.ONGOING
    }

    private fun isBoardFull(): Boolean {
        return board.all { row -> row.none { it == EMPTY } }
    }

    private fun updateBoardUI() {
        board.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, cell ->
                if (cell != EMPTY) updateCellUI(row, col, cell)
            }
        }
    }

    private fun updateCellUI(row: Int, col: Int, player: String) {
        val cellView = when (row to col) {
            0 to 0 -> ui.cell1
            0 to 1 -> ui.cell2
            0 to 2 -> ui.cell3
            1 to 0 -> ui.cell4
            1 to 1 -> ui.cell5
            1 to 2 -> ui.cell6
            2 to 0 -> ui.cell7
            2 to 1 -> ui.cell8
            2 to 2 -> ui.cell9
            else -> null
        }
        cellView?.setImageResource(when (player) {
            PLAYER -> R.drawable.ic_cross
            AI -> R.drawable.ic_zero
            else -> return
        })
    }

    private fun showGameMenu() {
        ui.gameTimer.stop()
        val timeElapsed = SystemClock.elapsedRealtime() - ui.gameTimer.base

        AlertDialog.Builder(this)
            .setView(R.layout.game_menu_dialog)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()

                findViewById<View>(R.id.resume_button)?.setOnClickListener {
                    ui.gameTimer.base = SystemClock.elapsedRealtime() - timeElapsed
                    ui.gameTimer.start()
                    dismiss()
                }

                findViewById<View>(R.id.settings_button)?.setOnClickListener {
                    settingsResult.launch(Intent(this@GameSession, GamePreferences::class.java))
                    dismiss()
                }

                findViewById<View>(R.id.quit_button)?.setOnClickListener {
                    saveGameState(timeElapsed)
                    finish()
                }
            }
    }

    private fun showResultDialog(result: GameResult) {
        gameActive = false
        ui.gameTimer.stop()

        if (gameRestored) clearSavedGame()

        AlertDialog.Builder(this)
            .setView(when (result) {
                GameResult.PLAYER_WIN -> R.layout.win_dialog
                GameResult.AI_WIN -> R.layout.lose_dialog
                GameResult.DRAW -> R.layout.draw_dialog
                else -> return
            })
            .setCancelable(false)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()

                findViewById<View>(R.id.dialog_ok)?.setOnClickListener {
                    dismiss()
                    finish()
                }
            }
    }

    private fun saveGameState(time: Long) {
        val gameState = board.joinToString("\n") { it.joinToString(";") }
        getSharedPreferences("game_data", MODE_PRIVATE).edit {
            putLong("last_game_time", time)
            putString("last_game_state", gameState)
        }
    }

    private fun clearSavedGame() {
        getSharedPreferences("game_data", MODE_PRIVATE).edit {
            putLong("last_game_time", 0)
            putString("last_game_state", "")
            apply()
        }
    }

    private fun loadSettings(): GamePreferences.GameSettings {
        with(getSharedPreferences("game_data", MODE_PRIVATE)) {
            return GamePreferences.GameSettings(
                getInt(PREF_VOLUME, 30),
                getInt(PREF_DIFFICULTY, 1),
                getInt(PREF_WIN_CONDITIONS, 7)
            )
        }
    }

    data class Move(val row: Int, val col: Int)

    enum class GameResult {
        PLAYER_WIN,
        AI_WIN,
        DRAW,
        ONGOING
    }

    companion object {
        const val PLAYER = "X"
        const val AI = "O"
        const val EMPTY = " "
    }
}