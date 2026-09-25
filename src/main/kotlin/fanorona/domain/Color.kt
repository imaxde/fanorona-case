package fanorona.domain

enum class Color {
    WHITE,
    BLACK;

    fun opposite(): Color = if (this == WHITE) BLACK else WHITE
}
