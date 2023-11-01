package actions

import CommandCallback
import models.Authors
import models.Publishes
import models.Reader
import utils.Database

object UserActions {
    val enterUser: CommandCallback = { state, (name) ->
//        println("Welcome to bookabase! To create your account, please answer the following questions:")
//        val statement = Database.connection.prepareStatement(
//            """
//                SELECT *
//                FROM authors a
//                INNER JOIN publishes p
//                    ON a.book_id = p.book_id
//                LIMIT 10
//            """.trimIndent(),
//        )
//
//        val (queryStatus, results) = Database.runQuery(statement, Authors::class, Publishes::class)
//
//        println(queryStatus)
//        results.forEach {
//            val authorCast = it as Authors
//            val publisherCast = it as Publishes
//            println("${authorCast.bookId} ${authorCast.contributorId} ${publisherCast.contributorId}")
//        }
        val nameString = name as String
        val statement = Database.connection.prepareStatement(
            """
                SELECT *
                FROM reader
                WHERE username = ?
            """.trimIndent()
        )
        statement.setString(1, nameString)
        var (status, results) = Database.runQuery(statement, Reader::class)

        println(results)
    }

    val exitUser: CommandCallback = { state, _ -> println("hello") }

    val listUser: CommandCallback = { state, _ -> }

    val followUser: CommandCallback = { state, _ -> }

    val unfollowUser: CommandCallback = { state, _ -> }
}