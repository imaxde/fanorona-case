package fanorona.domain

data class Player(val name: String) {
    init {
        require(name.isNotBlank()) { "Player name must not be blank" }
    }
}
