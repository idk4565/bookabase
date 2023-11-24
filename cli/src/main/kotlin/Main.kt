import actions.BookActions
import actions.CollectionActions
import actions.UserActions
import models.Book
import models.Collection
import models.Reader
import utils.Database
import utils.State

/**
 *     Available Commands:
 *         user
 *             enter [username]
 *                 search [username]
 *                 follow [username]
 *                 unfollow [username]
 *                 TODO Justin: collections
 *                 TODO Justin: following
 *                 TODO Justin: followers
 *                 TODO Justin: top10
 *                 TODO Sid: recommend [recent | followers | month | for_me]
 *                 exit
 *         collection
 *             list
 *             book [id]
 *             create "[name]"
 *             enter [id]
 *                 add [book_id]
 *                 remove [book_id]
 *                 rename "[new_name]"
 *                 exit
 *                 delete
 *         book
 *             search [name | authors | publishers | genre | rel_date_gt | rel_date_lt] [value] (sort=)[name | publisher | genre | rel_year | none] [asc | desc | none]
 *             read [id | random]
 *                 stop
 *             enter [id]
 *                 rate [rating]
 *                 exit
 *
 */

val alwaysTrueValidator: ArgumentValidator = { _ -> true }

fun main(args: Array<String>) {
    // shouldn't be run with any parameters
    if (args.isNotEmpty()) {
        println("Usage: bookabase")
        return
    }

    val commandManager = CommandManager(
        // State
        object : State {
            override var user: Reader? = null
            override var collection: Collection? = null
            override var book: Book? = null
        },
        // User Commands
        Command("user", "enter", listOf { it.isNotEmpty() && it.length <= 12 }, UserActions.enterUser),
        Command("user", "search", listOf { it.isNotEmpty() && it.length <= 64 }, UserActions.searchUsers),
        Command("user", "exit", listOf(), UserActions.exitUser),
        Command("user", "follow", listOf { it.isNotEmpty() && it.length <= 12 }, UserActions.followUser),
        Command("user", "unfollow", listOf { it.isNotEmpty() && it.length <= 12 }, UserActions.unfollowUser),
        // Collection Commands
        Command("collection", "list", listOf(), CollectionActions.listCollections),
        Command("collection", "books", listOf { it.toIntOrNull() != null }, CollectionActions.booksInCollection),
        Command("collection", "create", listOf { it.isNotEmpty() && it.length <= 64 }, CollectionActions.createCollection),
        Command("collection", "enter", listOf { it.toIntOrNull() != null }, CollectionActions.enterCollection),
        Command("collection", "rename", listOf { it.isNotEmpty() && it.length <= 64 }, CollectionActions.renameCollection),
        Command("collection", "add", listOf { it.toIntOrNull() != null }, CollectionActions.addToCollection),
        Command("collection", "remove", listOf { it.toIntOrNull() != null }, CollectionActions.removeFromCollection),
        Command("collection", "delete", listOf(), CollectionActions.deleteCollection),
        Command("collection", "exit", listOf(), CollectionActions.exitCollection),
        // Book Commands
        Command("book", "search", listOf(
            { it == "name" || it == "rel_date_lt" ||
                    it == "rel_date_gt" || it == "authors" ||
                    it == "publisher" || it == "genre" },
            alwaysTrueValidator,
            { it == "name" || it == "publisher" || it == "genre" || it == "rel_year" || it == "none" },
            { it == "asc" || it == "dsc" || it == "none" }
        ), BookActions.listBooks),
        Command("book", "enter", listOf { it.toIntOrNull() != null }, BookActions.enterBook),
        Command("book", "read", listOf(alwaysTrueValidator), BookActions.bookStartReading),
        Command("book", "stop", listOf(), BookActions.bookStopReading),
        Command("book", "rate", listOf { it.toIntOrNull() != null && it.toInt() >= 1 && it.toInt() <= 5 }, BookActions.rateBook),
        Command("book", "exit", listOf(), BookActions.exitBook),
    )

    while (true) {
        // take in command
        print("bookabase> ")
        val command: String = readln()
        if (command.isEmpty()) { continue }
        if (command == "exit") { break }

        commandManager.parseCommand(command)
    }

    Database.close()
}