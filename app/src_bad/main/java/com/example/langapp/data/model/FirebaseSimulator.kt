package com.example.langapp.data.model

object FirebaseSimulator {
    private val simulatedData = mapOf(
        "elementary" to mapOf(
            "2" to mapOf(
                "phonetics" to listOf(
                    mapOf(
                        "id" to "ph1",
                        "word" to "Яблоко",
                        "ipa" to "[ˈjæbləkəʊ]",
                        "audio" to "apple_pron",
                        "hint" to "Ударение на первый слог"
                    )
                ),
                "vocabulary" to listOf(
                    mapOf(
                        "id" to "v1",
                        "word" to "Стол",
                        "imageRes" to "img_table",
                        "translations" to mapOf("en" to "Table", "es" to "Mesa"),
                        "examples" to listOf(
                            "Накройте стол к ужину",
                            "Деревянный стол стоит в центре"
                        ),
                        "audioRes" to "audio_table"
                    )
                )
            )
        )
    )

    fun getTasks(level: String, lesson: Int, type: String): List<Task> {
        return simulatedData[level]?.get(lesson.toString())?.get(type)
            ?.map {
                when (type) {
                    "phonetics" -> PhoneticTask(
                        id = it["id"] as String,
                        word = it["word"] as String,
                        ipa = it["ipa"] as String,
                        audio = it["audio"] as String,
                        hint = it["hint"] as String
                    )
                    "vocabulary" -> VocabularyTask(
                        id = it["id"] as String,
                        word = it["word"] as String,
                        image = it["image"] as String,
                        translations = it["translations"] as Map<String, String>
                    )
                    else -> throw IllegalArgumentException("Unknown task type")
                }
            } ?: emptyList()
    }
}
