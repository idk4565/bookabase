package actions

import CommandCallback
import de.m3y.kformat.Table
import de.m3y.kformat.table
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.*
import models.Collection
import utils.Database
import java.io.File
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

object BookActions {
    @Serializable
    data class BookReadData(val bookId: Int, val startTime: Long, val startPage: Int)

    private val statusPath = Path(System.getProperty("user.dir"), "status.json")

    val listBooks: CommandCallback = start@{ state, (searchCriteria, searchValue, orderCriteria, orderType) ->
        val innerQuery = when (searchCriteria) {
            "name" -> {
                """
                        SELECT
                            b.book_id,
                            b.title,
                            array_to_string(ARRAY(SELECT c.name
                                                  FROM publishes p
                                                  INNER JOIN contributor c ON c.contributor_id = p.contributor_id
                                                  WHERE p.book_id = b.book_id
                                                  ORDER BY c.name), ', ') as computed3,
                            (SELECT audience_name FROM audience WHERE audience_id = b.audience_id) as computed4,
                            (SELECT genre_name FROM genre WHERE genre_id = b.genre_id) as computed5,
                            b.page_length,
                            b.release_date,
                            (SELECT COALESCE(AVG(rating), -1) FROM rates WHERE book_id = b.book_id)::int as computed2
                        FROM book b
                        WHERE LOWER(b.title) LIKE LOWER(?)
                        ORDER BY b.title ASC, b.release_date ASC
                    """.trimIndent()
            }

            "rel_date_gt" -> {
                """
                        SELECT
                            b.book_id,
                            b.title,
                            array_to_string(ARRAY(SELECT c.name
                                                  FROM publishes p
                                                  INNER JOIN contributor c ON c.contributor_id = p.contributor_id
                                                  WHERE p.book_id = b.book_id
                                                  ORDER BY c.name), ', ') as computed3,
                            (SELECT audience_name FROM audience WHERE audience_id = b.audience_id) as computed4,
                            (SELECT genre_name FROM genre WHERE genre_id = b.genre_id) as computed5,
                            b.page_length,
                            b.release_date,
                            (SELECT COALESCE(AVG(rating), -1) FROM rates WHERE book_id = b.book_id)::int as computed2
                        FROM book b
                        WHERE ? <= b.release_date
                        ORDER BY b.title ASC, b.release_date ASC
                    """.trimIndent()
            }

            "rel_date_lt" -> {
                """
                        SELECT
                            b.book_id,
                            b.title,
                            array_to_string(ARRAY(SELECT c.name
                                                  FROM publishes p
                                                  INNER JOIN contributor c ON c.contributor_id = p.contributor_id
                                                  WHERE p.book_id = b.book_id
                                                  ORDER BY c.name), ', ') as computed3,
                            (SELECT audience_name FROM audience WHERE audience_id = b.audience_id) as computed4,
                            (SELECT genre_name FROM genre WHERE genre_id = b.genre_id) as computed5,
                            b.page_length,
                            b.release_date,
                            (SELECT COALESCE(AVG(rating), -1) FROM rates WHERE book_id = b.book_id)::int as computed2
                        FROM book b
                        WHERE ? >= b.release_date
                        ORDER BY b.title ASC, b.release_date ASC
                    """.trimIndent()
            }

            "authors" -> {
                """
                        SELECT
                            b.book_id,
                            b.title,
                            array_to_string(ARRAY(SELECT c.name
                                                  FROM publishes p
                                                  INNER JOIN contributor c ON c.contributor_id = p.contributor_id
                                                  WHERE p.book_id = b.book_id
                                                  ORDER BY c.name), ', ') as computed3,
                            (SELECT audience_name FROM audience WHERE audience_id = b.audience_id) as computed4,
                            (SELECT genre_name FROM genre WHERE genre_id = b.genre_id) as computed5,
                            b.page_length,
                            b.release_date,
                            (SELECT COALESCE(AVG(rating), -1) FROM rates WHERE book_id = b.book_id)::int as computed2
                        FROM book b
                        INNER JOIN authors a
                            ON a.book_id = b.book_id
                        INNER JOIN contributor c
                            ON c.contributor_id = a.contributor_id
                        WHERE LOWER(c.name) LIKE LOWER(?)
                        ORDER BY b.title ASC, b.release_date ASC
                    """.trimIndent()
            }

            "publisher" -> {
                """
                        SELECT
                            b.book_id,
                            b.title,
                            array_to_string(ARRAY(SELECT c.name
                                                  FROM publishes p
                                                  INNER JOIN contributor c ON c.contributor_id = p.contributor_id
                                                  WHERE p.book_id = b.book_id
                                                  ORDER BY c.name), ', ') as computed3,
                            (SELECT audience_name FROM audience WHERE audience_id = b.audience_id) as computed4,
                            (SELECT genre_name FROM genre WHERE genre_id = b.genre_id) as computed5,
                            b.page_length,
                            b.release_date,
                            (SELECT COALESCE(AVG(rating), -1) FROM rates WHERE book_id = b.book_id)::int as computed2
                        FROM book b
                        INNER JOIN publishes p
                            ON p.book_id = b.book_id
                        INNER JOIN contributor c
                            ON c.contributor_id = p.contributor_id
                        WHERE LOWER(c.name) LIKE LOWER(?)
                        ORDER BY b.title ASC, b.release_date ASC
                    """.trimIndent()
            }

            else -> {
                """
                        SELECT
                            b.book_id,
                            b.title,
                            array_to_string(ARRAY(SELECT c.name
                                                  FROM publishes p
                                                  INNER JOIN contributor c ON c.contributor_id = p.contributor_id
                                                  WHERE p.book_id = b.book_id
                                                  ORDER BY c.name), ', ') as computed3,
                            (SELECT audience_name FROM audience WHERE audience_id = b.audience_id) as computed4,
                            g.genre_name,
                            b.page_length,
                            b.release_date,
                            (SELECT COALESCE(AVG(rating), -1) FROM rates WHERE book_id = b.book_id)::int as computed2
                        FROM book b
                        INNER JOIN genre g
                            ON b.genre_id = g.genre_id
                        WHERE LOWER(g.genre_name) LIKE LOWER(?)
                        ORDER BY b.title ASC, b.release_date ASC
                    """.trimIndent()
            }
        }

        val orderByStatement =  when (orderCriteria) {
            "name" -> "title"
            "publisher" -> "computed3"
            "genre" -> when (searchCriteria) {
                "genre" -> "genre_name"
                else -> "computed5"
            }
            "rel_year" -> "release_date"
            else -> ""
        }

        val orderByDirection = when (orderType) {
            "asc" -> "ASC"
            "dsc" -> "DESC"
            else -> ""
        }

        val completeOrderByStatement = if (orderByStatement.isNotEmpty() && orderByDirection.isNotEmpty()) {
            "ORDER BY " + when (orderByStatement) {
                "release_date" -> "EXTRACT('YEAR' FROM t.release_date)"
                else -> orderByStatement
            } + " $orderByDirection"
        } else {
            ""
        }

        val preparedQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM ( $innerQuery ) t
                $completeOrderByStatement
            """.trimIndent()
        )

        // set val
        when (searchCriteria) {
            "name" -> preparedQuery.setString(1, "$searchValue%")
            "rel_date_gt" -> preparedQuery.setDate(1, java.sql.Date.valueOf(searchValue))
            "rel_date_lt" -> preparedQuery.setDate(1, java.sql.Date.valueOf(searchValue))
            "authors" -> preparedQuery.setString(1, "$searchValue%")
            "publisher" -> preparedQuery.setString(1, "$searchValue%")
            else -> preparedQuery.setString(1, "$searchValue%")
        }
        val (_, searchResult) = Database.runQuery(
            preparedQuery, *when (searchCriteria) {
                "name" -> arrayOf(Book::class, Computed::class)
                "rel_date_lt" -> arrayOf(Book::class, Computed::class)
                "rel_date_gt" -> arrayOf(Book::class, Computed::class)
                "authors" -> arrayOf(Book::class, Computed::class, Contributor::class)
                "publisher" -> arrayOf(Book::class, Computed::class, Contributor::class)
                else -> arrayOf(Book::class, Computed::class, Genre::class)
            }
        )

        println()
        table {
            header("ID", "Title", "Publisher", "Audience", "Genre", "Page Length", "Release Date", "Rating")
            searchResult.forEach {
                val asBook = it as Book
                val asComputed = it as Computed

                when (searchCriteria) {
                    "genre" -> {
                        this.row(
                            asBook.id,
                            asBook.title,
                            asComputed.computed3,
                            asComputed.computed4,
                            (it as Genre).name,
                            asBook.pageLength,
                            asBook.releaseDate,
                            if (asComputed.computed2 != -1) "${asComputed.computed2}/5" else "N/A"
                        )
                    }

                    else -> {
                        this.row(
                            asBook.id,
                            asBook.title,
                            asComputed.computed3,
                            asComputed.computed4,
                            asComputed.computed5,
                            asBook.pageLength,
                            asBook.releaseDate,
                            if (asComputed.computed2 != -1) "${asComputed.computed2}/5" else "N/A"
                        )
                    }
                }
            }

            hints {
                alignment("Title", Table.Hints.Alignment.LEFT)
                alignment("Publisher", Table.Hints.Alignment.LEFT)
                alignment("Audience", Table.Hints.Alignment.LEFT)
                alignment("Genre", Table.Hints.Alignment.LEFT)
                alignment("Release Date", Table.Hints.Alignment.LEFT)

                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
        }.print(System.out)
    }

    val enterBook: CommandCallback = start@{ state, (id) ->
        if (state.user == null) {
            println("You must be logged in to enter a book!")
            return@start
        }

        // check if book exists
        val idAsInt = id.toInt()
        val bookExistsQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM book
                WHERE book_id = ?
            """.trimIndent()
        )
        bookExistsQuery.setInt(1, idAsInt)
        val (_, bookExistsResult) = Database.runQuery(bookExistsQuery, Book::class)
        if (bookExistsResult.isEmpty()) {
            println("Book with id $idAsInt does not exist!")
            return@start
        }

        // else
        println("Successfully entered book with id $idAsInt!")
        state.book = bookExistsResult.first() as Book
    }

    val bookStartReading: CommandCallback = start@{ state, (action) ->
        if (state.user == null) {
            println("You must be logged in to read a book!")
            return@start
        }

        // decide action
        val book = if (action.toIntOrNull() != null) {
            val bookExistsQuery = Database.connection.prepareStatement(
                """
                    SELECT *
                    FROM book
                    WHERE book_id = ?
                """.trimIndent()
            )
            bookExistsQuery.setInt(1, action.toInt())
            val (_, bookExistsResults) = Database.runQuery(bookExistsQuery, Book::class)
            if (bookExistsResults.isEmpty()) {
                println("Unable to find book with ${action.toInt()}!")
                return@start
            } else {
                bookExistsResults.first() as Book
            }
        } else if (action == "random") {
            val randomBookQuery = Database.connection.prepareStatement(
                """
                    SELECT b.*
                    FROM collection coll
                    INNER JOIN (
                        SELECT *
                        FROM contains
                        WHERE reader_id = ?
                    ) cont
                        ON coll.collection_id = cont.collection_id
                    INNER JOIN book b
                        ON cont.book_id = b.book_id
                    ORDER BY random()
                    LIMIT 1
                """.trimIndent()
            )
            randomBookQuery.setInt(1, state.user!!.id)
            val (_, randomBookResult) = Database.runQuery(randomBookQuery, Book::class)
            if (randomBookResult.isEmpty()) {
                println("Unable to find a random book to read!")
                return@start
            } else {
                randomBookResult.first() as Book
            }
        } else {
            println("Unknown action '$action' passed to read!")
            return@start
        }

        var statusData = mutableMapOf<Int, BookReadData>()
        if (statusPath.exists()) {
            statusData = Json.decodeFromString<MutableMap<Int, BookReadData>>(File(statusPath.toString()).readText())
        }

        if (statusData.containsKey(state.user!!.id)) {
            val getBookQuery = Database.connection.prepareStatement(
                """
                    SELECT *
                    FROM book
                    WHERE book_id = ?
                """.trimIndent()
            )
            getBookQuery.setInt(1, statusData[state.user!!.id]!!.bookId)
            val (_, getBookResult) = Database.runQuery(getBookQuery, Book::class)
            if (getBookResult.isNotEmpty()) {
                println(
                    "You cannot read two books at the same time! You are currently " + "reading ${(getBookResult.first() as Book).title}, which you started " + "on ${
                        Date.from(
                            Instant.ofEpochMilli(statusData[state.user!!.id]!!.startTime)
                        )
                    }."
                )
                return@start
            } else {
                statusData.remove(state.user!!.id)
            }
        }

        println("You have selected '${book.title}' that has a length of ${book.pageLength}!")
        var startPage: String? = null
        while (startPage == null) {
            startPage = getInput("Please enter start page: ", 10)
            if (!startPage.isNullOrEmpty() && startPage.toIntOrNull() != null && startPage.toInt() < book.pageLength) break
        }

        val startTime = Instant.now().toEpochMilli()
        statusData[state.user!!.id] = BookReadData(book.id, startTime, startPage!!.toInt())
        statusPath.writeText(Json.encodeToString(statusData))
        println(
            "Started reading '${book.title}' from page " + "$startPage at ${Date.from(Instant.ofEpochMilli(startTime))}!"
        )
    }

    val bookStopReading: CommandCallback = start@{ state, _ ->
        if (state.user == null) {
            println("You cannot stop reading if you're not logged in!")
            return@start
        }

        // get all statuses
        var statusData = mutableMapOf<Int, BookReadData>()
        if (statusPath.exists()) {
            statusData = Json.decodeFromString<MutableMap<Int, BookReadData>>(File(statusPath.toString()).readText())
        }

        val book = if (!statusData.containsKey(state.user!!.id)) {
            println("You are currently not reading any book!")
            return@start
        } else {
            val getBookQuery = Database.connection.prepareStatement(
                """
                    SELECT *
                    FROM book
                    WHERE book_id = ?
                """.trimIndent()
            )
            getBookQuery.setInt(1, statusData[state.user!!.id]!!.bookId)
            val (_, getBookResult) = Database.runQuery(getBookQuery, Book::class)
            if (getBookResult.isNotEmpty()) {
                (getBookResult.first() as Book)
            } else {
                statusData.remove(state.user!!.id)
                return@start
            }
        }

        val startTime = statusData[state.user!!.id]!!.startTime
        val endTime = Instant.now().toEpochMilli()
        val startPage = statusData[state.user!!.id]!!.startPage

        println(
            "You are currently reading '${book.title}' starting from page $startPage. " + "The book has a length of ${book.pageLength}!"
        )
        var endPage: String? = null
        while (endPage == null) {
            endPage = getInput("Please enter end page: ", 10)
            if (!endPage.isNullOrEmpty() && endPage.toIntOrNull() != null && endPage.toInt() <= book.pageLength) break
        }

        // Insert into rads
        val readsQuery = Database.connection.prepareStatement(
            """
                INSERT INTO reads(start_time, end_time, start_page, end_page, reader_id, book_id) 
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING *
            """.trimIndent()
        )
        readsQuery.setTimestamp(1, Timestamp.from(Instant.ofEpochMilli(startTime)))
        readsQuery.setTimestamp(2, Timestamp.from(Instant.ofEpochMilli(endTime)))
        readsQuery.setInt(3, startPage)
        readsQuery.setInt(4, endPage!!.toInt())
        readsQuery.setInt(5, state.user!!.id)
        readsQuery.setInt(6, book.id)
        val (_, readsResult) = Database.runQuery(readsQuery, Reads::class)
        if (readsResult.isEmpty()) {
            println("Error registering read with the database!")
            return@start
        } else {
            println(
                "Successfully logged reading session into the database. Session Info:\n" + "  Book: ${book.title}\n" + "  Start Time: ${
                    Timestamp.from(
                        Instant.ofEpochMilli(startTime)
                    )
                }\n" + "  End Time: ${Timestamp.from(Instant.ofEpochMilli(endTime))}\n" + "  Start Page: $startPage\n" + "  End Page: $endPage"
            )
        }

        statusData.remove(state.user!!.id)
        statusPath.writeText(Json.encodeToString(statusData))
    }

    val rateBook: CommandCallback = start@{ state, (rating) ->
        // prelim checks
        if (state.user == null) {
            println("You must be logged in to rate a book!")
            return@start
        }

        if (state.book == null) {
            println("You must be in a book to rate it!")
            return@start
        }

        // check if we already rated
        val ratingAsInt = rating.toInt()
        val alreadyRatedQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM rates
                WHERE book_id = ? AND reader_id = ?
            """.trimIndent()
        )
        alreadyRatedQuery.setInt(1, state.book!!.id)
        alreadyRatedQuery.setInt(2, state.user!!.id)
        val (_, alreadyRatedResults) = Database.runQuery(alreadyRatedQuery, Rates::class)

        // if rated
        if (alreadyRatedResults.isNotEmpty()) {
            val updateRatingQuery = Database.connection.prepareStatement(
                """
                    UPDATE rates
                    SET rating = ?
                    WHERE book_id = ? and reader_id = ?
                    RETURNING *
                """.trimIndent()
            )
            updateRatingQuery.setInt(1, ratingAsInt)
            updateRatingQuery.setInt(2, state.book!!.id)
            updateRatingQuery.setInt(3, state.user!!.id)
            val (_, updateRatingResults) = Database.runQuery(updateRatingQuery, Rates::class)
            if (updateRatingResults.isEmpty()) {
                println("Failed to update rating for book with id ${state.book!!.id}!")
            } else {
                println("Updating rating for book with id ${state.book!!.id} to $ratingAsInt/5 from ${(alreadyRatedResults.first() as Rates).rating}/5!")
            }

            return@start
        }

        // insert new rating
        val newRatingQuery = Database.connection.prepareStatement(
            """
                INSERT INTO rates(reader_id, book_id, rating)
                VALUES (?, ?, ?)
                RETURNING *
            """.trimIndent()
        )
        newRatingQuery.setInt(1, state.user!!.id)
        newRatingQuery.setInt(2, state.book!!.id)
        newRatingQuery.setInt(3, ratingAsInt)
        val (_, newRatingResults) = Database.runQuery(newRatingQuery, Rates::class)
        if (newRatingResults.isEmpty()) {
            println("Failed to rate book with id ${state.book!!.id}!")
        } else {
            println("Successfully rated book with id ${state.book!!.id} a $ratingAsInt/5!")
        }
    }

    val exitBook: CommandCallback = start@{ state, _ ->
        if (state.book == null) {
            println("You are not in any book!")
            return@start
        }

        println("Successfully exited book with id ${state.book!!.id}!")
        state.book = null
    }

    private fun getInput(prompt: String, length: Int): String? {
        print(prompt)
        val result = readln()
        if (result.isEmpty() || result.length > length) {
            return null
        }

        return result
    }
}