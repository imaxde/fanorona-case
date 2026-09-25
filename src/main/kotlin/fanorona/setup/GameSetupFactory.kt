package fanorona.setup

import fanorona.domain.Arrangement
import fanorona.domain.Board
import fanorona.rules.ActionRule
import fanorona.rules.DrawRule

interface GameSetupFactory {
    fun createBoard(): Board

    fun createInitialArrangement(board: Board): Arrangement

    fun createDrawRules(): List<DrawRule>

    fun createActionRules(): List<ActionRule>
}
