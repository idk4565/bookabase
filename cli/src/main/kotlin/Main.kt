import actions.BookActions
import actions.CollectionActions
import actions.UserActions
import models.Book
import models.Collection
import models.Reader
import org.apache.commons.validator.routines.EmailValidator
import utils.Database
import utils.State
import kotlin.io.path.Path

/**
 *     Available Commands:
 *       user
 *         list
 *         enter [name]
 *           follow [email]
 *           unfollow [email]
 *         exit
 *       collection
 *         list
 *         enter [name]
 *           update [new_name]
 *           add [book_id]
 *           remove [book_id]
 *           delete
 *         exit
 *       book
 *         list [rel_date | authors | publishers | length | audience | rating]=value sort=[rel_date | authors | publishers | length | audience | rating] asc | desc
 *         enter [id | random]
 *           rate [rating]
 *           read [start_page] [end_page] [start_time] [end_time]
 *         exit
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
        Command("user", "enter", listOf { it.isNotEmpty() && it.length <= 64 }, UserActions.enterUser),
        Command("user", "search", listOf { it.isNotEmpty() && it.length <= 64 }, UserActions.searchUsers),
        Command("user", "exit", listOf(), UserActions.exitUser),
        Command("user", "follow", listOf { EmailValidator.getInstance().isValid(it) }, UserActions.followUser),
        Command("user", "unfollow", listOf { EmailValidator.getInstance().isValid(it) }, UserActions.unfollowUser),
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