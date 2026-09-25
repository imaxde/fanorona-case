package fanorona.ui

object CommandParser {
    fun parse(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val token = StringBuilder()
        var quoted = false
        var escaped = false
        var started = false
        for (char in line) {
            when {
                escaped -> {
                    token.append(char)
                    escaped = false
                    started = true
                }
                quoted && char == '\\' -> escaped = true
                char == '"' -> {
                    quoted = !quoted
                    started = true
                }
                char.isWhitespace() && !quoted -> {
                    if (started) {
                        tokens += token.toString()
                        token.clear()
                        started = false
                    }
                }
                else -> {
                    token.append(char)
                    started = true
                }
            }
        }
        require(!quoted && !escaped) { "Незакрытые кавычки в команде" }
        if (started) tokens += token.toString()
        return tokens
    }
}
