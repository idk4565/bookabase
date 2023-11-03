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

    val listBooks: CommandCallback = start@{ state, (searchCriteria, searchValue, sortCriteria, sortOrder) ->
        if (state.user == null) {
            println("You must be logged in to search books!")
            return@start
        }

        /*
        Users will be able to search for books by name, release date, authors, publisher, or
        genre. The resulting list of books must show the book’s name, the authors, the pub-
        lisher, the length, audience and the ratings. The list must be sorted alphabetically
        (ascending) by book’s name and release date. Users can sort the resulting list: book
        name, publisher, genre, and released year (ascending and descending)
         */
        //check what they searched by
        var searchQueryBuilder =
                """
                    SELECT b.title, 
                        (
                            SELECT c.name 
                            FROM authors a
                            INNER JOIN contributor c
                            ON a.contributor_id = c.contributor_id
                            WHERE a.book_id = b.book_id
                        ) as computed1,
                        (
                            SELECT c.name 
                            FROM publishes p
                            INNER JOIN contributor c
                            ON p.contributor_id = c.contributor_id
                            WHERE p.book_id = b.book_id 
                        ) as computed2, 
                        b.page_length, 
                        aud.audience_name
                    FROM book b 
                    LEFT JOIN audience aud
                        ON aud.audience_id = b.audience_id
                    INNER JOIN publishes pub
                        ON b.book_id = pub.book_id
                    INNER JOIN contributor publisher
                        ON publisher.contributor_id = pub.contributor_id
                    WHERE $searchCriteria LIKE '%$searchValue%'
                """.trimIndent()

        // check if they wanted to sort
        searchQueryBuilder += if (sortCriteria == "") {
            " ORDER BY title, release date"
        }
        else {
            " ORDER BY $sortCriteria"
        }

        // check for asc vs dsc
        searchQueryBuilder += if (sortOrder == "") {
            " ASC"
        }
        else {
            " $sortOrder"
        }

        val bookExistsQuery = Database.connection.prepareStatement(searchQueryBuilder)

        val (_, bookExistsResult) = Database.runQuery(
                bookExistsQuery,
                Book::class,
                Audience::class,
                Computed::class
        )
        if (bookExistsResult.isEmpty()) {
            println("No results found")
            return@start
        }

        // else if results found
        println()
        table {
            header("Title", "Authors", "Publisher", "Page Length", "Audience", "Ratings")
            bookExistsResult.map {
                val asBook = it as Book
                val asAudience = it as Audience
                val asComputed = it as Computed
                this.row(
                        asBook.title,
                        asComputed.computed1,
                        asComputed.computed2,
                        asBook.pageLength,
                        asAudience.name,
                        //asComputed.computed3
                )
            }

            hints {
                alignment("Title", Table.Hints.Alignment.LEFT)
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
                    "You cannot read two books at the same time! You are currently " +
                            "reading ${(getBookResult.first() as Book).title}, which you started " +
                            "on ${Date.from(Instant.ofEpochMilli(statusData[state.user!!.id]!!.startTime))}."
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
            if (!startPage.isNullOrEmpty() &&
                startPage.toIntOrNull() != null &&
                startPage.toInt() < book.pageLength) break
        }

        val startTime = Instant.now().toEpochMilli()
        statusData[state.user!!.id] = BookReadData(book.id, startTime, startPage!!.toInt())
        statusPath.writeText(Json.encodeToString(statusData))
        println("Started reading '${book.title}' from page " +
                "$startPage at ${Date.from(Instant.ofEpochMilli(startTime))}!")
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

        println("You are currently reading '${book.title}' starting from page $startPage. " +
                "The book has a length of ${book.pageLength}!")
        var endPage: String? = null
        while (endPage == null) {
            endPage = getInput("Please enter end page: ", 10)
            if (!endPage.isNullOrEmpty() &&
                endPage.toIntOrNull() != null &&
                endPage.toInt() <= book.pageLength) break
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
            println("Successfully logged reading session into the database. Session Info:\n" +
                    "  Book: ${book.title}\n" +
                    "  Start Time: ${Timestamp.from(Instant.ofEpochMilli(startTime))}\n" +
                    "  End Time: ${Timestamp.from(Instant.ofEpochMilli(endTime))}\n" +
                    "  Start Page: $startPage\n" +
                    "  End Page: $endPage")
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