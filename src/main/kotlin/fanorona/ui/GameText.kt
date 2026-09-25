package fanorona.ui

import fanorona.domain.Color
import fanorona.domain.Outcome

internal object GameText {
    fun side(color: Color): String = if (color == Color.WHITE) "белые" else "чёрные"

    fun outcome(outcome: Outcome): String = when (outcome) {
        Outcome.WHITE_WIN -> "Победа белых"
        Outcome.BLACK_WIN -> "Победа чёрных"
        Outcome.DRAW -> "Ничья"
    }
}
