package com.example.my_tick_tac_toe

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.my_tick_tac_toe.databinding.PreferencesScreenBinding

const val PREF_VOLUME = "com.game.tictactoe.VOLUME"
const val PREF_DIFFICULTY = "com.game.tictactoe.DIFFICULTY"
const val PREF_WIN_CONDITIONS = "com.game.tictactoe.WIN_CONDITIONS"

class GamePreferences : AppCompatActivity() {
    private lateinit var ui: PreferencesScreenBinding
    private var difficulty = 0
    private var volume = 0
    private var winConditions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = PreferencesScreenBinding.inflate(layoutInflater)

        val currentPrefs = loadPreferences()
        difficulty = currentPrefs.difficulty
        volume = currentPrefs.volume
        winConditions = currentPrefs.winConditions

        setupDifficultyControls()
        setupVolumeControl()
        setupWinConditionToggles()
        setupNavigation()

        setContentView(ui.root)
    }

    private fun setupDifficultyControls() {
        ui.difficultyText.text = resources.getStringArray(R.array.difficulty_levels)[difficulty]
        updateDifficultyButtons()

        ui.decreaseDifficulty.setOnClickListener {
            difficulty--
            updateDifficultyState()
        }

        ui.increaseDifficulty.setOnClickListener {
            difficulty++
            updateDifficultyState()
        }
    }

    private fun updateDifficultyState() {
        updateDifficultyButtons()
        ui.difficultyText.text = resources.getStringArray(R.array.difficulty_levels)[difficulty]
        saveDifficulty(difficulty)
    }

    private fun updateDifficultyButtons() {
        ui.decreaseDifficulty.visibility = if (difficulty == 0) View.INVISIBLE else View.VISIBLE
        ui.increaseDifficulty.visibility = if (difficulty == 2) View.INVISIBLE else View.VISIBLE
    }

    private fun setupVolumeControl() {
        ui.volumeSlider.progress = volume
        ui.volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volume = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveVolume(volume)
            }
        })
    }

    private fun setupWinConditionToggles() {
        ui.toggleHorizontal.isChecked = winConditions and 1 != 0
        ui.toggleVertical.isChecked = winConditions and 2 != 0
        ui.toggleDiagonal.isChecked = winConditions and 4 != 0

        ui.toggleHorizontal.setOnCheckedChangeListener { _, checked ->
            winConditions = if (checked) winConditions or 1 else winConditions and 6
            saveWinConditions(winConditions)
        }

        ui.toggleVertical.setOnCheckedChangeListener { _, checked ->
            winConditions = if (checked) winConditions or 2 else winConditions and 5
            saveWinConditions(winConditions)
        }

        ui.toggleDiagonal.setOnCheckedChangeListener { _, checked ->
            winConditions = if (checked) winConditions or 4 else winConditions and 3
            saveWinConditions(winConditions)
        }
    }

    private fun setupNavigation() {
        ui.backButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun saveVolume(value: Int) {
        getSharedPreferences("game_data", MODE_PRIVATE).edit {
            putInt(PREF_VOLUME, value)
        }
        setResult(RESULT_OK)
    }

    private fun saveDifficulty(level: Int) {
        getSharedPreferences("game_data", MODE_PRIVATE).edit {
            putInt(PREF_DIFFICULTY, level)
        }
        setResult(RESULT_OK)
    }

    private fun saveWinConditions(conditions: Int) {
        getSharedPreferences("game_data", MODE_PRIVATE).edit {
            putInt(PREF_WIN_CONDITIONS, conditions)
        }
        setResult(RESULT_OK)
    }

    private fun loadPreferences(): GameSettings {
        with(getSharedPreferences("game_data", MODE_PRIVATE)) {
            return GameSettings(
                getInt(PREF_VOLUME, 30),
                getInt(PREF_DIFFICULTY, 1),
                getInt(PREF_WIN_CONDITIONS, 7)
            )
        }
    }

    data class GameSettings(val volume: Int, val difficulty: Int, val winConditions: Int)
}