package actions

import CommandCallback
import de.m3y.kformat.Table
import de.m3y.kformat.table
import models.*
import models.Collection
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
            println("You already follow $usernameAsString!")
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
            println("You are not following $usernameAsString!")
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

    val findMyCollectionCount: CommandCallback = start@{ state, _ ->
        if (state.user == null) {
            println("You must be logged in to get your collection count!")
            return@start
        }

        val collectionCountQuery = Database.connection.prepareStatement(
            """
                SELECT COUNT(*)::INTEGER as computed1
                FROM collection
                WHERE reader_id = ?
            """.trimIndent()
        )
        collectionCountQuery.setInt(1, state.user!!.id)
        val (_, queryResult) = Database.runQuery(collectionCountQuery, Collection::class, Computed::class)
        val resultCount = (queryResult.first() as Computed).computed1
        if (resultCount == 1) println("You have 1 collection")
        else println("You have $resultCount collections")
        return@start
    }

    val findMyFollowingCount: CommandCallback = start@{ state, _ ->
        if (state.user == null) {
            println("You must be logged in to get the count of users you follow!")
            return@start
        }

        val followingCountQuery = Database.connection.prepareStatement(
            """
                SELECT COUNT(*)::INTEGER as computed1
                FROM follows
                WHERE follower_id = ?
            """.trimIndent()
        )
        followingCountQuery.setInt(1, state.user!!.id)
        val (_, queryResult) = Database.runQuery(followingCountQuery, Follows::class, Computed::class)
        val resultCount = (queryResult.first() as Computed).computed1
        if (resultCount == 1) println("You are following 1 user")
        else println("You are following $resultCount users")
        return@start
    }

    val findMyFollowerCount: CommandCallback = start@{ state, _ ->
        if (state.user == null) {
            println("You must be logged in to get your follower count!")
            return@start
        }

        val followerCountQuery = Database.connection.prepareStatement(
            """
                SELECT COUNT(*)::INTEGER as computed1
                FROM follows
                WHERE followee_id = ?
            """.trimIndent()
        )
        followerCountQuery.setInt(1, state.user!!.id)
        val (_, queryResult) = Database.runQuery(followerCountQuery, Follows::class, Computed::class)
        val resultCount = (queryResult.first() as Computed).computed1
        if (resultCount == 1) println("You have 1 follower")
        else println("You have $resultCount followers")
        return@start
    }

    val findMyTop10: CommandCallback = start@{ state, _ ->
        if (state.user == null) {
            println("You must be logged in to see your top 10 books!")
            return@start
        }

        val top10BooksQuery = Database.connection.prepareStatement(
            """
                SELECT b.title, b.page_length, b.release_date,
                    COALESCE(MAX(ra.rating), -1)::INTEGER AS computed1
                FROM book b
                LEFT JOIN reads r 
                    ON b.book_id = r.book_id
                LEFT JOIN rates ra 
                    ON b.book_id = ra.book_id AND ra.reader_id = ?
                WHERE r.reader_id = ?
                GROUP BY b.book_id, b.title, b.release_date
                ORDER BY computed1 DESC
                LIMIT 10;
            """.trimIndent()
        )
        top10BooksQuery.setInt(1, state.user!!.id)
        top10BooksQuery.setInt(2, state.user!!.id)
        val (_, queryResult) = Database.runQuery(top10BooksQuery, Book::class, Reads::class, Rates::class, Computed::class)

        if (queryResult.isEmpty()) {
            println("You haven't read any books")
            return@start
        } else if (queryResult.size == 1) {
            println("You haven't read 10 books, but here's your top " + queryResult.size + " book")
        }
        else if (queryResult.size < 10){
            println("You haven't read 10 books, but here's your top " + queryResult.size + " books")
        }

        println()
        table {
            header("Title", "Page Length", "Release Date", "Rating")
            queryResult.map {
                val asBook = it as Book
                val asComputed = it as Computed
                //if rating wasn't given, let's replace that with a space
                if (asComputed.computed1 == -1) {
                    this.row(asBook.title, asBook.pageLength, asBook.releaseDate, "N/A")
                } else {
                    this.row(asBook.title, asBook.pageLength, asBook.releaseDate, "${asComputed.computed1}/5")
                }
            }

            hints {
                alignment("Title", Table.Hints.Alignment.LEFT)
                alignment("Page Length", Table.Hints.Alignment.LEFT)
                alignment("Release Date", Table.Hints.Alignment.LEFT)
                alignment("Rating", Table.Hints.Alignment.LEFT)
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
        }.print(System.out)
        return@start
    }

    private fun getInput(prompt: String, length: Int): String? {
        print(prompt)
        val result = readln()
        if (result.isEmpty() || result.length > length) { return null }

        return result
    }
}