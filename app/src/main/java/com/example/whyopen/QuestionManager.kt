package com.example.whyopen

import kotlin.random.Random

enum class QuestionType { REFLECTIVE, KNOWLEDGE, LOGIC, MATH }

data class Question(
    val text: String,
    val answer: String? = null,
    val type: QuestionType,
    val hint: String? = null
)

class QuestionManager {
    fun getRandomQuestion(): Question {
        val rand = Random.nextInt(100)
        return try {
            when {
                rand < 40 -> getReflectiveQuestion()
                rand < 75 -> getKnowledgeQuestion()
                rand < 90 -> getLogicQuestion()
                else -> getMathQuestion()
            }
        } catch (e: Exception) {
            Question("Why are you opening this app?", type = QuestionType.REFLECTIVE)
        }
    }

    private fun getReflectiveQuestion(): Question {
        val q = listOf(
            "Why are you opening this app right now?",
            "Will this help your goals today?",
            "What do you plan to achieve here?",
            "How will you feel after spending 30 minutes on this?",
            "Is there something more important you should be doing?",
            "Are you seeking a distraction from a difficult task?"
        )
        return Question(q.random(), type = QuestionType.REFLECTIVE)
    }

    private fun getKnowledgeQuestion(): Question {
        val q = mapOf(
            "Which country has the largest population?" to "India",
            "What is the capital of Australia?" to "Canberra",
            "Which planet has the most moons?" to "Saturn",
            "Who wrote 'Romeo and Juliet'?" to "Shakespeare",
            "What is the chemical symbol for Gold?" to "Au",
            "In which year did World War II end?" to "1945"
        )
        val entry = q.entries.random()
        return Question(entry.key, entry.value, QuestionType.KNOWLEDGE)
    }

    private fun getLogicQuestion(): Question {
        val rand = Random.nextInt(2)
        return if (rand == 0) {
            val words = mapOf("TIVYPRODUCTI" to "PRODUCTIVITY", "CUSFO" to "FOCUS", "CIPLINEDI" to "DISCIPLINE")
            val entry = words.entries.random()
            Question("Unscramble this word: ${entry.key}", entry.value, QuestionType.LOGIC)
        } else {
            val series = listOf("2, 4, 8, 16", "1, 3, 9, 27", "5, 10, 20, 40")
            val answers = listOf("32", "81", "80")
            val idx = Random.nextInt(series.size)
            Question("What comes next: ${series[idx]}?", answers[idx], QuestionType.LOGIC)
        }
    }

    private fun getMathQuestion(): Question {
        val a = Random.nextInt(12, 20)
        val b = Random.nextInt(7, 13)
        return Question("What is $a × $b?", (a * b).toString(), QuestionType.MATH)
    }
}
