package com.example

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.databinding.ActivityMainBinding
import java.util.Locale

/**
 * Data model for a supported TTS language.
 */
data class TtsLanguage(
    val displayName: String,
    val locale: Locale,
    val sampleText: String
) {
    override fun toString(): String = displayName
}

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady: Boolean = false
    private var isCurrentlySpeaking: Boolean = false

    private val utteranceId = "VoiceCraft_Utterance_ID"

    // Supported languages list, prioritizing Bengali and English as requested
    private val languagesList: List<TtsLanguage> by lazy {
        listOf(
            TtsLanguage(
                displayName = "Bengali (Bangladesh)",
                locale = Locale("bn", "BD"),
                sampleText = getString(R.string.bengali_sample_text)
            ),
            TtsLanguage(
                displayName = "Bengali (India)",
                locale = Locale("bn", "IN"),
                sampleText = "নমস্কার! কেমন আছেন? এটি একটি নেটিভ অ্যান্ড্রয়েড অফলাইন বাংলা টেক্সট টু স্পিচ পরীক্ষা।"
            ),
            TtsLanguage(
                displayName = "English (United States)",
                locale = Locale.US,
                sampleText = getString(R.string.english_sample_text)
            ),
            TtsLanguage(
                displayName = "English (United Kingdom)",
                locale = Locale.UK,
                sampleText = "Good day! Welcome to offline native Text-to-Speech synthesis on Android."
            ),
            TtsLanguage(
                displayName = "English (India)",
                locale = Locale("en", "IN"),
                sampleText = "Hello and welcome! This offline speech synthesizer is working seamlessly."
            ),
            TtsLanguage(
                displayName = "Hindi (India)",
                locale = Locale("hi", "IN"),
                sampleText = "नमस्ते! आप कैसे हैं? यह एंड्रॉइड का नेटिव ऑफलाइन टेक्स्ट टू स्पीच इंजन है।"
            ),
            TtsLanguage(
                displayName = "Spanish (Spain)",
                locale = Locale("es", "ES"),
                sampleText = "¡Hola! Bienvenido a la síntesis de texto a voz sin conexión en Android."
            ),
            TtsLanguage(
                displayName = "French (France)",
                locale = Locale.FRANCE,
                sampleText = "Bonjour! Bienvenue dans l'application de synthèse vocale hors ligne."
            ),
            TtsLanguage(
                displayName = "German (Germany)",
                locale = Locale.GERMANY,
                sampleText = "Guten Tag! Willkommen bei der nativen Text-to-Speech-Sprachsynthese."
            ),
            TtsLanguage(
                displayName = "Japanese (Japan)",
                locale = Locale.JAPAN,
                sampleText = "こんにちは！オフラインの音声読み上げアプリへようこそ。"
            ),
            TtsLanguage(
                displayName = "Arabic",
                locale = Locale("ar"),
                sampleText = "مرحباً بكم في تطبيق تحويل النص إلى كلام باللغة العربية على أندرويد."
            )
        )
    }

    private var selectedLanguageIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupTextCounters()
        setupLanguageSpinner()
        setupSliders()
        setupButtons()

        // Initialize Android's native offline TextToSpeech engine
        updateStatus(
            statusText = getString(R.string.status_initializing),
            isSuccess = false,
            isWarning = false
        )
        textToSpeech = TextToSpeech(this, this)
    }

    /**
     * Window insets handling for modern edge-to-edge layout.
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    /**
     * Initializes TextToSpeech engine callback.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true

            // Set utterance progress listener for real-time speech tracking
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread {
                        isCurrentlySpeaking = true
                        updateSpeakingUiState(true)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.endsWith("_last") != true) return
                    runOnUiThread {
                        isCurrentlySpeaking = false
                        updateSpeakingUiState(false)
                        updateStatus(
                            statusText = getString(R.string.status_stopped),
                            isSuccess = true,
                            isWarning = false
                        )
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        isCurrentlySpeaking = false
                        updateSpeakingUiState(false)
                        updateStatus(
                            statusText = "Playback error occurred",
                            isSuccess = false,
                            isWarning = true
                        )
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    runOnUiThread {
                        isCurrentlySpeaking = false
                        updateSpeakingUiState(false)
                        updateStatus(
                            statusText = "Speech error (Code: $errorCode)",
                            isSuccess = false,
                            isWarning = true
                        )
                    }
                }
            })

            // Apply selected language and slider configs
            applySelectedLanguage()
            applySpeechRate()
            applyPitch()

        } else {
            isTtsReady = false
            updateStatus(
                statusText = getString(R.string.msg_tts_error),
                isSuccess = false,
                isWarning = true
            )
            binding.cardWarning.visibility = View.VISIBLE
            binding.tvWarningText.text = getString(R.string.msg_tts_error)
            binding.btnInstallVoice.setOnClickListener { openTtsSettings() }
            Toast.makeText(this, R.string.msg_tts_error, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Sets up the language selector dropdown Spinner.
     */
    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languagesList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerLanguage.adapter = adapter
        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguageIndex = position
                if (isTtsReady) {
                    applySelectedLanguage()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Applies the selected language and verifies whether language voice data exists.
     */
    private fun applySelectedLanguage() {
        val tts = textToSpeech ?: return
        val currentLang = languagesList[selectedLanguageIndex]

        val result = tts.setLanguage(currentLang.locale)

        when (result) {
            TextToSpeech.LANG_MISSING_DATA -> {
                binding.cardWarning.visibility = View.VISIBLE
                binding.tvWarningText.text = "${currentLang.displayName}: Voice data missing. Tap below to install."
                binding.btnInstallVoice.setOnClickListener { openTtsInstallOrSettings() }
                updateStatus(
                    statusText = "Missing Voice Data",
                    isSuccess = false,
                    isWarning = true
                )
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                binding.cardWarning.visibility = View.VISIBLE
                binding.tvWarningText.text = "${currentLang.displayName} is not supported by device TTS engine."
                binding.btnInstallVoice.setOnClickListener { openTtsSettings() }
                updateStatus(
                    statusText = "Language Unsupported",
                    isSuccess = false,
                    isWarning = true
                )
            }
            else -> {
                // Supported and ready
                binding.cardWarning.visibility = View.GONE
                updateStatus(
                    statusText = getString(R.string.status_ready),
                    isSuccess = true,
                    isWarning = false
                )
            }
        }
    }

    /**
     * Sets up Speed (Rate) and Pitch SeekBars.
     */
    private fun setupSliders() {
        // Speed slider: progress 0..200 -> rate 0.5f to 2.5f (50 = 1.0f)
        binding.sbSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = calculateRate(progress)
                val descriptor = when {
                    rate < 0.9f -> "(Slow)"
                    rate in 0.9f..1.1f -> "(Normal)"
                    else -> "(Fast)"
                }
                binding.tvSpeedValue.text = String.format(Locale.US, "Speed: %.2fx %s", rate, descriptor)
                if (isTtsReady) {
                    textToSpeech?.setSpeechRate(rate)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Pitch slider: progress 0..150 -> pitch 0.5f to 2.0f (50 = 1.0f)
        binding.sbPitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pitch = calculatePitch(progress)
                val descriptor = when {
                    pitch < 0.9f -> "(Deep)"
                    pitch in 0.9f..1.1f -> "(Normal)"
                    else -> "(High)"
                }
                binding.tvPitchValue.text = String.format(Locale.US, "Pitch: %.2fx %s", pitch, descriptor)
                if (isTtsReady) {
                    textToSpeech?.setPitch(pitch)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set initial label displays
        binding.sbSpeed.progress = 50
        binding.sbPitch.progress = 50
    }

    private fun calculateRate(progress: Int): Float {
        // progress 0 -> 0.5f, 50 -> 1.0f, 200 -> 2.5f
        return 0.5f + (progress / 100f) * 1.0f
    }

    private fun calculatePitch(progress: Int): Float {
        // progress 0 -> 0.5f, 50 -> 1.0f, 150 -> 2.0f
        return 0.5f + (progress / 100f) * 1.0f
    }

    private fun applySpeechRate() {
        val rate = calculateRate(binding.sbSpeed.progress)
        textToSpeech?.setSpeechRate(rate)
    }

    private fun applyPitch() {
        val pitch = calculatePitch(binding.sbPitch.progress)
        textToSpeech?.setPitch(pitch)
    }

    /**
     * Sets up click listeners for all interactive buttons.
     */
    private fun setupButtons() {
        binding.btnVoice.setOnClickListener {
            showVoicePicker()
        }
        // Speak Primary Button
        binding.btnSpeak.setOnClickListener {
            speakCurrentText()
        }

        // Stop Secondary Button
        binding.btnStop.setOnClickListener {
            stopSpeech()
        }

        // Clear Button
        binding.btnClear.setOnClickListener {
            binding.etInputText.setText("")
            stopSpeech()
        }

        // Paste from Clipboard Button
        binding.btnPaste.setOnClickListener {
            pasteFromClipboard()
        }

        // Quick Test: Bengali Sample Button
        binding.btnSampleBengali.setOnClickListener {
            // Find Bengali (BD) in list
            val bnIndex = languagesList.indexOfFirst { it.locale.language == "bn" }
            if (bnIndex != -1) {
                binding.spinnerLanguage.setSelection(bnIndex)
            }
            binding.etInputText.setText(getString(R.string.bengali_sample_text))
            binding.etInputText.setSelection(binding.etInputText.text?.length ?: 0)
        }

        // Quick Test: English Sample Button
        binding.btnSampleEnglish.setOnClickListener {
            // Find English (US) in list
            val enIndex = languagesList.indexOfFirst { it.locale == Locale.US }
            if (enIndex != -1) {
                binding.spinnerLanguage.setSelection(enIndex)
            }
            binding.etInputText.setText(getString(R.string.english_sample_text))
            binding.etInputText.setSelection(binding.etInputText.text?.length ?: 0)
        }

        // Reset Sliders Button
        binding.btnResetControls.setOnClickListener {
            binding.sbSpeed.progress = 50
            binding.sbPitch.progress = 50
            Toast.makeText(this, "Controls reset to 1.0x defaults", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Splits long text into smaller chunks at sentence ends.
     */
    private fun splitIntoChunks(text: String, maxLen: Int = 1500): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.length > maxLen) {
            var cut = remaining.lastIndexOfAny(charArrayOf('.', '!', '?', '।', '\n'), maxLen)
            if (cut < maxLen / 2) cut = remaining.lastIndexOf(' ', maxLen)
            if (cut <= 0) cut = maxLen
            chunks.add(remaining.substring(0, cut + 1).trim())
            remaining = remaining.substring(cut + 1).trim()
        }
        if (remaining.isNotEmpty()) chunks.add(remaining)
        return chunks.filter { it.isNotBlank() }
    }

    /**
     * Shows a list of installed voices for the selected language.
     */
    private fun showVoicePicker() {
        val tts = textToSpeech
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TTS Engine is initializing, please wait…", Toast.LENGTH_SHORT).show()
            return
        }

        val lang = languagesList[selectedLanguageIndex].locale.language
        val voices = try {
            tts.voices?.filter { it.locale.language == lang }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        if (voices.isEmpty()) {
            Toast.makeText(this, "No voices found for this language", Toast.LENGTH_LONG).show()
            return
        }

        val names = voices.mapIndexed { index, v ->
            "Voice ${index + 1}  (${v.name})"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Choose voice")
            .setItems(names) { _, which ->
                tts.voice = voices[which]
                Toast.makeText(this, "Voice ${which + 1} selected", Toast.LENGTH_SHORT).show()
                speakCurrentText()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Executes the Text-to-Speech synthesis using Android's native offline engine.
     */
    private fun speakCurrentText() {
        val textToSpeak = binding.etInputText.text?.toString()?.trim() ?: ""

        if (textToSpeak.isEmpty()) {
            Toast.makeText(this, R.string.msg_empty_text, Toast.LENGTH_SHORT).show()
            binding.etInputText.requestFocus()
            return
        }

        if (!isTtsReady || textToSpeech == null) {
            Toast.makeText(this, "TTS Engine is initializing, please wait…", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure rate & pitch are up to date
        applySpeechRate()
        applyPitch()

        // Speak in chunks so very long text works
        val chunks = splitIntoChunks(textToSpeak)
        var hadError = false
        chunks.forEachIndexed { index, chunk ->
            val id = if (index == chunks.lastIndex) "${utteranceId}_last" else "${utteranceId}_$index"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
            }
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (textToSpeech?.speak(chunk, mode, params, id) == TextToSpeech.ERROR) {
                hadError = true
            }
        }

        if (hadError) {
            Toast.makeText(this, "Failed to speak text. Check language voice data.", Toast.LENGTH_SHORT).show()
            updateSpeakingUiState(false)
        } else {
            updateSpeakingUiState(true)
        }
    }

    /**
     * Stops any ongoing speech synthesis immediately.
     */
    private fun stopSpeech() {
        if (textToSpeech != null && textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        isCurrentlySpeaking = false
        updateSpeakingUiState(false)
        updateStatus(
            statusText = getString(R.string.status_ready),
            isSuccess = true,
            isWarning = false
        )
    }

    /**
     * Pastes text from Android system clipboard.
     */
    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this)
                if (!text.isNullOrEmpty()) {
                    binding.etInputText.setText(text)
                    binding.etInputText.setSelection(text.length)
                    Toast.makeText(this, R.string.msg_pasted, Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
        Toast.makeText(this, R.string.msg_clipboard_empty, Toast.LENGTH_SHORT).show()
    }

    /**
     * Monitors character and word counts in the EditText.
     */
    private fun setupTextCounters() {
        binding.etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val content = s?.toString()?.trim() ?: ""
                val charCount = content.length
                val wordCount = if (content.isEmpty()) 0 else content.split("\\s+".toRegex()).size
                binding.tvCharCount.text = getString(R.string.char_count_format, charCount, wordCount)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Updates the status badge UI.
     */
    private fun updateStatus(statusText: String, isSuccess: Boolean, isWarning: Boolean) {
        binding.tvStatus.text = statusText
        when {
            isSuccess -> {
                binding.layoutStatusBadge.setBackgroundResource(R.drawable.bg_status_badge)
                binding.ivStatusDot.setImageResource(R.drawable.ic_check_circle)
                binding.ivStatusDot.imageTintList = ColorStateList.valueOf(getColor(R.color.on_success_container))
                binding.tvStatus.setTextColor(getColor(R.color.on_success_container))
            }
            isWarning -> {
                binding.layoutStatusBadge.setBackgroundResource(R.drawable.bg_warning_banner)
                binding.ivStatusDot.setImageResource(R.drawable.ic_warning)
                binding.ivStatusDot.imageTintList = ColorStateList.valueOf(getColor(R.color.on_warning_container))
                binding.tvStatus.setTextColor(getColor(R.color.on_warning_container))
            }
            else -> {
                binding.layoutStatusBadge.setBackgroundResource(R.drawable.bg_spinner_border)
                binding.ivStatusDot.setImageResource(R.drawable.ic_speed)
                binding.ivStatusDot.imageTintList = ColorStateList.valueOf(getColor(R.color.text_secondary))
                binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
            }
        }
    }

    /**
     * Updates UI components while speech synthesis is active.
     */
    private fun updateSpeakingUiState(speaking: Boolean) {
        binding.progressBarSpeaking.visibility = if (speaking) View.VISIBLE else View.INVISIBLE
        if (speaking) {
            binding.btnSpeak.setIconResource(R.drawable.ic_volume_up)
            binding.btnSpeak.text = "Speaking…"
            updateStatus(
                statusText = getString(R.string.status_speaking),
                isSuccess = true,
                isWarning = false
            )
        } else {
            binding.btnSpeak.setIconResource(R.drawable.ic_volume_up)
            binding.btnSpeak.text = getString(R.string.btn_speak)
        }
    }

    /**
     * Opens Android Voice Data install intent or falls back to system TTS settings.
     */
    private fun openTtsInstallOrSettings() {
        try {
            val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            startActivity(installIntent)
        } catch (e: Exception) {
            openTtsSettings()
        }
    }

    /**
     * Opens Android System Text-to-Speech Settings.
     */
    private fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Unable to open device settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Cleanup TextToSpeech resource on activity destruction.
     */
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
