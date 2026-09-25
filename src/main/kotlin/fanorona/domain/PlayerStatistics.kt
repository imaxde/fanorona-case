package fanorona.domain

data class PlayerStatistics(val wins: Int, val losses: Int, val games: Int) {
    val draws: Int
        get() = games - wins - losses
}
