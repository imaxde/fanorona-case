package fanorona.rules

import fanorona.domain.Game

fun interface DrawRule {
    fun isDraw(game: Game): Boolean
}

class NoCaptureLimitRule(val limit: Int = 40) : DrawRule {
    init {
        require(limit > 0)
    }

    override fun isDraw(game: Game): Boolean = game.movesWithoutCapture() >= limit
}

class RepetitionRule(val times: Int = 3) : DrawRule {
    init {
        require(times > 1)
    }

    override fun isDraw(game: Game): Boolean = game.repetitionCount() >= times
}
