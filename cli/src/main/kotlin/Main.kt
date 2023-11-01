import actions.UserActions
import org.apache.commons.validator.routines.EmailValidator
import utils.Database
import utils.State

/**
 * create -> Create
 *      user -> Create User
 *      collection -> Create Collection
 * login -> Login
 * logout -> Logout
 * collections -> Gets all collections
 * delete collection -> Delete collection
 * update collection -> Update collection
 * rate -> Rate book
 * read -> Read book
 * search -> Search
 *      user -> Search user
 *      book -> Search book
 * follow -> Follow
 * unfollow -> Unfollow
 */

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
            override var userId: Int = -1
            override var collectionId: Int = -1
            override var bookId: Int = -1
        },
        // User Commands
        Command("user", "enter", listOf(
            Pair(String::class) { (it as String).length <= 14 }
        ), UserActions.enterUser),
        Command("user", "exit", listOf(), UserActions.exitUser),
        Command("user", "list", listOf(), UserActions.listUser),
        Command("user", "follow", listOf(
            Pair(String::class) { EmailValidator.getInstance().isValid(it as String) }
        ), UserActions.followUser),
        Command("user", "unfollow", listOf(
            Pair(String::class) { EmailValidator.getInstance().isValid(it as String) }
        ), UserActions.unfollowUser),
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