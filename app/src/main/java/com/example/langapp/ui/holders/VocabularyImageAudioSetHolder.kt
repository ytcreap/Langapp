package com.example.langapp.ui.holders

import android.media.MediaPlayer
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.VocabularyImageAudioSet
import com.example.langapp.data.model.VocabularyItem
import com.example.langapp.ui.adapters.VocabularyAdapter
import java.text.Collator
import java.util.Locale

class VocabularyImageAudioSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val vocabularyRecyclerView: RecyclerView = view.findViewById(R.id.vocabularyRecyclerView)

    private var mediaPlayer: MediaPlayer? = null

    fun bind(vocabularySet: VocabularyImageAudioSet) {
        // Устанавливаем заголовок и вопрос
        titleText.text = vocabularySet.taskname
        questionText.text = vocabularySet.question

        // Создаем Collator для русской локали
        val russianCollator = Collator.getInstance(Locale("ru", "RU"))
        russianCollator.strength = Collator.PRIMARY

        // Преобразуем Map в список VocabularyItem и сортируем по алфавиту
        val vocabularyList = vocabularySet.vocabulary.entries
            .mapNotNull { (word, data) ->
                val image = data["image"] ?: ""
                val sound = data["sound"] ?: ""

                // Если нет изображения, используем пустую строку
                if (word.isNotEmpty() && sound.isNotEmpty()) {
                    VocabularyItem(word, image, sound)
                } else {
                    null
                }
            }
            .sortedWith { item1, item2 ->
                russianCollator.compare(
                    normalizeWordForSorting(item1.word),
                    normalizeWordForSorting(item2.word)
                )
            }

        // Для отладки
        println("Загружено слов для '${vocabularySet.taskname}': ${vocabularyList.size}")
        vocabularyList.forEachIndexed { index, item ->
            println("${index + 1}. ${item.word} (аудио: ${item.audio.isNotEmpty()}, изображение: ${item.image.isNotEmpty()})")
        }

        // Проверяем, что список не пустой
        if (vocabularyList.isEmpty()) {
            vocabularyRecyclerView.visibility = View.GONE
            titleText.text = "${vocabularySet.taskname} (нет данных)"
            return
        }

        // Настраиваем RecyclerView с вертикальным LinearLayoutManager
        val layoutManager = LinearLayoutManager(itemView.context)
        vocabularyRecyclerView.layoutManager = layoutManager
        vocabularyRecyclerView.setHasFixedSize(false)

        // Создаем адаптер
        val adapter = VocabularyAdapter(
            items = vocabularyList,
            onPlayClick = { audioUrl ->
                playAudio(audioUrl)
            }
        )

        vocabularyRecyclerView.adapter = adapter
    }

    /**
     * Нормализация слова для сортировки
     */
    private fun normalizeWordForSorting(word: String): String {
        return word
            .lowercase(Locale("ru", "RU"))
            .replace("ё", "е")
            .replace(Regex("[́`'.,!?]"), "")
            .trim()
    }

    private fun playAudio(audioUrl: String) {
        // Останавливаем предыдущее воспроизведение
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener {
                    it.start()
                }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, what, extra ->
                    println("Ошибка воспроизведения аудио: what=$what, extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Ошибка при создании MediaPlayer: ${e.message}")
                release()
                mediaPlayer = null
            }
        }
    }

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}