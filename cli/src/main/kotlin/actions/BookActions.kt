package actions

import CommandCallback

object BookActions {
    val listBooks: CommandCallback = { state, _ -> }

    val enterBook: CommandCallback = { state, _ -> }

    val readBook: CommandCallback = { state, _ -> }

    val rateBook: CommandCallback = { state, _ -> }

    val exitBook: CommandCallback = { state, _ -> }
}