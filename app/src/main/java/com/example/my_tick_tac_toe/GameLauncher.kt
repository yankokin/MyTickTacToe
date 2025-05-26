package com.example.my_tick_tac_toe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.my_tick_tac_toe.databinding.LauncherScreenBinding

const val SAVED_GAME_TIME = "com.example.my_tick_tac_toe.GAME_TIME"
const val SAVED_GAME_STATE = "com.example.my_tick_tac_toe.GAME_STATE"

class GameLauncher : AppCompatActivity() {
    private lateinit var ui: LauncherScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Base_Theme_MyTickTacToe)
        super.onCreate(savedInstanceState)

        ui = LauncherScreenBinding.inflate(layoutInflater)

        ui.newGameButton.setOnClickListener {
            startActivity(Intent(this, GameSession::class.java))
        }

        ui.continueButton.setOnClickListener {
            val lastGame = loadLastGame()
            Intent(this, GameSession::class.java).apply {
                putExtra(SAVED_GAME_TIME, lastGame.time)
                putExtra(SAVED_GAME_STATE, lastGame.state)
                startActivity(this)
            }
        }

        ui.settingsButton.setOnClickListener {
            startActivity(Intent(this, GamePreferences::class.java))
        }

        setContentView(ui.root)
    }

    private fun loadLastGame(): SavedGame {
        with(getSharedPreferences("game_data", MODE_PRIVATE)) {
            return SavedGame(
                getLong("last_game_time", 0),
                getString("last_game_state", "") ?: ""
            )
        }
    }

    data class SavedGame(val time: Long, val state: String)
}