package actions

import Command
import CommandCallback
import de.m3y.kformat.Table
import de.m3y.kformat.table
import models.Follows
import models.Reader
import utils.Database
import java.sql.Timestamp
import java.time.Instant

object UserActions {
    val enterUser: CommandCallback = start@ { state, (username) ->
        if (state.user != null) {
            println("There is already a logged in user.")
            return@start
        }

        val statement = Database.connection.prepareStatement(
            """
                SELECT *
                FROM reader
                WHERE username = ?
            """.trimIndent()
        )
        statement.setString(1, username)
        val (_, selectResult) = Database.runQuery(statement, Reader::class)

        // if the user exists
        if (selectResult.isNotEmpty()) {
            val reader = selectResult.first() as Reader
            var tries = 0
            while (true) {
                if (tries == 3) {
                    println("user login: 3 incorrect password attempts")
                    return@start
                }

                val password = getInput("Enter your password: ", 48)
                if (reader.password == password) break
                ++tries
            }

            val updateStatement = Database.connection.prepareStatement(
                """
                    UPDATE reader
                    SET last_access_date = ?
                    WHERE reader_id = ?
                    RETURNING *
                """.trimIndent()
            )
            updateStatement.setTimestamp(1, Timestamp(Instant.now().toEpochMilli()))
            updateStatement.setInt(2, reader.id)
            val (_, updateResult) = Database.runQuery(updateStatement, Reader::class)

            state.user = (updateResult.first() as Reader)
            println("Welcome back!")

            return@start
        }

        // if the user doesn't
        println("Welcome to bookabase! It looks like you don't have an account yet. " +
                "To get started, please answer the following questions: ")
        val firstName = getInput("First Name: ", 48)
        val lastName = getInput("Last Name: ", 48)
        val email = getInput("Email: ", 64)
        val password = getInput("Password: ", 48)

        // insert new user
        val newUserStatement = Database.connection.prepareStatement(
            """
                INSERT INTO reader(username, password, firstname, lastname, email, creation_date, last_access_date)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING *
            """.trimIndent()
        )
        newUserStatement.setString(1, username)
        newUserStatement.setString(2, password)
        newUserStatement.setString(3, firstName)
        newUserStatement.setString(4, lastName)
        newUserStatement.setString(5, email)
        newUserStatement.setTimestamp(6, Timestamp(Instant.now().toEpochMilli()))
        newUserStatement.setTimestamp(7, Timestamp(Instant.now().toEpochMilli()))

        val (_, insertResult) = Database.runQuery(newUserStatement, Reader::class)
        state.user = (insertResult.first() as Reader)
    }

    val searchUsers: CommandCallback = start@ { state, (username) ->
        if (state.user == null) {
            println("You must be logged in to search users!")
            return@start
        }

        // Get users with like username
        val searchUserQuery = Database.connection.prepareStatement(
            """
                SELECT firstname, lastname, username
                FROM reader
                WHERE LOWER(username) LIKE LOWER(?)
            """.trimIndent()
        )
        searchUserQuery.setString(1, "$username%")
        val (_, searchUserResults) = Database.runQuery(searchUserQuery, Reader::class)
        if (searchUserResults.isEmpty()) {
            println("No users found with that username!")
            return@start
        }

        println()
        table {
            header("First Name", "Last Name", "Username")
            searchUserResults.forEach {
                val asReader = it as Reader
                this.row(asReader.firstName, asReader.lastName, asReader.username)
            }

            hints {
                alignment("First Name", Table.Hints.Alignment.LEFT)
                alignment("Last Name", Table.Hints.Alignment.LEFT)
                alignment("Username", Table.Hints.Alignment.LEFT)
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
        }.print(System.out)
    }

    val exitUser: CommandCallback = { state, _ ->
        println("Exited user ${state.user!!.username}!")
        state.user = null
        state.collection = null
    }

    val followUser: CommandCallback = start@ { state, (username) ->
        val usernameAsString = username
        if (state.user == null) {
            println("You must be logged in to follow a user!")
            return@start
        }

        // check if followed user exists
        val userExistsQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM reader
                WHERE username = ?
            """.trimIndent()
        )
        userExistsQuery.setString(1, usernameAsString)
        val (_, existsResult) = Database.runQuery(userExistsQuery, Reader::class)
        if (existsResult.isEmpty()) {
            println("The user you want to follow does not exist!")
            return@start
        }

        // check if we already follow the user
        val followUser = existsResult.first() as Reader
        val alreadyFollowQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM follows
                WHERE follower_id = ? AND followee_id = ?
            """.trimIndent()
        )
        alreadyFollowQuery.setInt(1, state.user!!.id)
        alreadyFollowQuery.setInt(2, followUser.id)
        val (_, alreadyFollowResult) = Database.runQuery(alreadyFollowQuery, Follows::class)
        if (alreadyFollowResult.isNotEmpty()) {
            println("You already follow $usernameAsString!");
            return@start
        }

        // else
        val followQuery = Database.connection.prepareStatement(
            """
                INSERT INTO follows(follower_id, followee_id)
                VALUES (?, ?)
                RETURNING *
            """.trimIndent()
        )
        followQuery.setInt(1, state.user!!.id)
        followQuery.setInt(2, followUser.id)
        val (followStatus, _) = Database.runQuery(followQuery, Follows::class)

        if (followStatus == Database.QueryStatus.SUCCESS) {
            println("You have successfully followed ${followUser.username}")
        } else {
            println("Unable to follow ${followUser.username}")
        }
    }

    val unfollowUser: CommandCallback = start@ { state, (username) ->
        val usernameAsString = username
        if (state.user == null) {
            println("You must be logged in to unfollow a user!")
            return@start
        }

        // check if followed user exists
        val userExistsQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM reader
                WHERE username = ?
            """.trimIndent()
        )
        userExistsQuery.setString(1, usernameAsString)
        val (_, existsResult) = Database.runQuery(userExistsQuery, Reader::class)
        if (existsResult.isEmpty()) {
            println("The user you want to unfollow does not exist!")
            return@start
        }

        // check if we dont follow the user
        val unfollowUser = existsResult.first() as Reader
        val notFollowedQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM follows
                WHERE follower_id = ? AND followee_id = ?
            """.trimIndent()
        )
        notFollowedQuery.setInt(1, state.user!!.id)
        notFollowedQuery.setInt(2, unfollowUser.id)
        val (_, alreadyFollowResult) = Database.runQuery(notFollowedQuery, Follows::class)
        if (alreadyFollowResult.isEmpty()) {
            println("You are not following $usernameAsString!");
            return@start
        }

        // else
        val unfollowQuery = Database.connection.prepareStatement(
            """
                DELETE FROM follows
                WHERE follower_id = ? AND followee_id = ?
                RETURNING *
            """.trimIndent()
        )
        unfollowQuery.setInt(1, state.user!!.id)
        unfollowQuery.setInt(2, unfollowUser.id)
        val (unfollowStatus, _) = Database.runQuery(unfollowQuery, Follows::class)

        if (unfollowStatus == Database.QueryStatus.SUCCESS) {
            println("You have successfully unfollowed ${unfollowUser.username}")
        } else {
            println("Unable to unfollow ${unfollowUser.username}")
        }
    }

    private fun getInput(prompt: String, length: Int): String? {
        print(prompt)
        val result = readln()
        if (result.isEmpty() || result.length > length) { return null }

        return result
    }
}