package actions

import CommandCallback
import de.m3y.kformat.Table
import de.m3y.kformat.table
import models.*
import models.Collection
import utils.Database
import utils.State

object CollectionActions {
    val listCollections: CommandCallback = start@ { state, _ ->
        if (state.user == null) {
            println("You need to be logged in to see your collections!")
            return@start
        }

        val collectionQuery = Database.connection.prepareStatement(
            """
                SELECT coll.collection_id,
                       coll.collection_name,
                       COUNT(b)::INTEGER as computed1,
                       COALESCE(SUM(page_length), 0)::INTEGER as computed2
                FROM (
                    SELECT *
                    FROM collection
                    WHERE reader_id = ?
                ) coll
                LEFT JOIN contains cont
                    ON coll.collection_id = cont.collection_id
                LEFT JOIN book b
                    ON cont.book_id = b.book_id
                GROUP BY coll.collection_id, coll.collection_name
                ORDER BY coll.collection_name ASC
            """.trimIndent()
        )
        collectionQuery.setInt(1, state.user!!.id)
        val (_, collections) = Database.runQuery(collectionQuery, Collection::class, Computed::class)
        if (collections.isEmpty()) {
            println("You have no collections!")
            return@start
        }

        println()
        table {
            header("ID", "Name", "# of Books", "Total Length")
            collections.map {
                val asColl = it as Collection
                val asComputed = it as Computed
                this.row(asColl.id, asColl.name, asComputed.computed1, asComputed.computed2)
            }

            hints {
                alignment("Name", Table.Hints.Alignment.LEFT)
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
        }.print(System.out)
    }

    val booksInCollection: CommandCallback = start@ { state, (id) ->
        // prelims
        if (state.user == null) {
            println("You need to be logged in to see the books in a collection!")
            return@start
        }

        val idAsInt = id.toInt()
        val allBookQuery = Database.connection.prepareStatement(
            """
                SELECT b.book_id,
                       b.title,
                       (SELECT audience_name
                        FROM audience a
                        WHERE a.audience_id = b.audience_id) as computed3,
                       (SELECT genre_name
                        FROM genre g
                        WHERE g.genre_id = b.genre_id) as computed4,
                       b.page_length,
                       b.release_date
                FROM (
                    SELECT *
                    FROM contains
                    WHERE reader_id = ? AND collection_id = ?
                ) cont
                INNER JOIN book b
                    ON cont.book_id = b.book_id
            """.trimIndent()
        )
        allBookQuery.setInt(1, state.user!!.id)
        allBookQuery.setInt(2, idAsInt)
        val (_, allBooksResult) = Database.runQuery(allBookQuery, Book::class, Computed::class)

        println()
        table {
            header("ID", "Title", "Audience", "Genre", "Page Length", "Release Date")
            allBooksResult.map {
                val asBook = it as Book
                val asComputed = it as Computed
                this.row(asBook.id, asBook.title, asComputed.computed3, asComputed.computed4, asBook.pageLength, asBook.releaseDate)
            }

            hints {
                alignment("Title", Table.Hints.Alignment.LEFT)
                alignment("Audience", Table.Hints.Alignment.LEFT)
                alignment("Genre", Table.Hints.Alignment.LEFT)
                alignment("Release Date", Table.Hints.Alignment.LEFT)
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
        }.print(System.out)
    }

    val createCollection: CommandCallback = start@ { state, (name) ->
        if (state.user == null) {
            println("You need to be logged in to enter a collection!")
            return@start
        }

        // make sure it doesn't exist
        val collectionExistsQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM collection
                WHERE collection_name = ?
            """.trimIndent()
        )
        collectionExistsQuery.setString(1, name)
        val (_, collectionExistsResult) = Database.runQuery(collectionExistsQuery, Collection::class)
        if (collectionExistsResult.isNotEmpty()) {
            println("Collection with name \"$name\" already exists! " +
                    "Its id is ${(collectionExistsResult.first() as Collection).id}")
            return@start
        }

        // else
        val newCollectionQuery = Database.connection.prepareStatement(
            """
                INSERT INTO collection(collection_name, reader_id)
                VALUES (?, ?)
                RETURNING *
            """.trimIndent()
        )
        newCollectionQuery.setString(1, name)
        newCollectionQuery.setInt(2, state.user!!.id)
        val (_, newCollectionResult) = Database.runQuery(newCollectionQuery, Collection::class)
        if (newCollectionResult.isEmpty()) {
            println("Unable to create new collection with name $name!")
            return@start
        }

        println("Successfully created collection with name $name! " +
                "It's id is ${(newCollectionResult.first() as Collection).id}. " +
                "Use this id if you want to enter it. You can also find the id by running " +
                "`collection list`.")
    }

    val enterCollection: CommandCallback = start@ { state, (id) ->
        if (state.user == null) {
            println("You need to be logged in to enter a collection!")
            return@start
        }

        val idAsInt = id.toInt()
        val collectionExistsQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM collection
                WHERE collection_id = ? AND reader_id = ?
            """.trimIndent()
        )
        collectionExistsQuery.setInt(1, idAsInt)
        collectionExistsQuery.setInt(2, state.user!!.id)

        val (_, existsResult) = Database.runQuery(collectionExistsQuery, Collection::class)
        if (existsResult.isEmpty()) {
            println("Collection with $idAsInt does not exist!")
            return@start
        }

        state.collection = existsResult.first() as Collection
        println("Successfully entered collection ${state.collection!!.name}!")
        return@start
    }

    val renameCollection: CommandCallback = start@ { state, (name) ->
        // prelim checks
        if (!defaultChecks(state, "rename")) { return@start }

        val originalName = state.collection!!.name
        val renameStatement = Database.connection.prepareStatement(
            """
                UPDATE collection
                SET collection_name = ?
                WHERE collection_id = ?
                RETURNING *
            """.trimIndent()
        )
        renameStatement.setString(1, name)
        renameStatement.setInt(2, state.collection!!.id)

        val (_, renameResult) = Database.runQuery(renameStatement, Collection::class)
        state.collection = renameResult.first() as Collection
        println("Collection $originalName renamed to $name")
    }

    val addToCollection: CommandCallback = start@ { state, (id) ->
        // prelim checks
        if (!defaultChecks(state, "add to")) { return@start }

        val bookId = id.toInt()
        val bookExistsQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM book
                WHERE book_id = ?
            """.trimIndent()
        )
        bookExistsQuery.setInt(1, bookId)
        val (_, bookExistsResult) = Database.runQuery(bookExistsQuery, Book::class)
        if (bookExistsResult.isEmpty()) {
            println("Book with id $bookId does not exist!")
            return@start
        }

        // if book is in query
        val bookAlreadyInCollectionQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM contains
                WHERE collection_id = ? AND book_id = ? AND reader_id = ?
            """.trimIndent()
        )
        bookAlreadyInCollectionQuery.setInt(1, state.collection!!.id)
        bookAlreadyInCollectionQuery.setInt(2, bookId)
        bookAlreadyInCollectionQuery.setInt(3, state.user!!.id)
        val (_, bookAlreadyInCollectionResult) = Database.runQuery(bookAlreadyInCollectionQuery, Contains::class)
        if (bookAlreadyInCollectionResult.isNotEmpty()) {
            println("Book with id $bookId is already in collection!")
            return@start
        }

        // else
        val insertBookInCollectionQuery = Database.connection.prepareStatement(
            """
                INSERT INTO contains(collection_id, book_id, reader_id)
                VALUES (?, ?, ?)
                RETURNING *
            """.trimIndent()
        )
        insertBookInCollectionQuery.setInt(1, state.collection!!.id)
        insertBookInCollectionQuery.setInt(2, bookId)
        insertBookInCollectionQuery.setInt(3, state.user!!.id)
        val (_, insertBookInCollectionResult) = Database.runQuery(insertBookInCollectionQuery, Contains::class)
        if (insertBookInCollectionResult.isEmpty()) {
            println("Failed adding book with id $bookId to collection with id ${state.collection!!.id}")
            return@start
        }

        println("Successfully added book with id $bookId to collection with id ${state.collection!!.id}")
    }

    val removeFromCollection: CommandCallback = start@ { state, (id) ->
        // prelim checks
        if (!defaultChecks(state, "remove from")) { return@start }

        // if book is in query
        val bookId = id.toInt()
        val bookInCollectionQuery = Database.connection.prepareStatement(
            """
                SELECT *
                FROM contains
                WHERE collection_id = ? AND book_id = ? AND reader_id = ?
            """.trimIndent()
        )
        bookInCollectionQuery.setInt(1, state.collection!!.id)
        bookInCollectionQuery.setInt(2, bookId)
        bookInCollectionQuery.setInt(3, state.user!!.id)
        val (_, bookInCollectionResult) = Database.runQuery(bookInCollectionQuery, Contains::class)
        if (bookInCollectionResult.isEmpty()) {
            println("Book with id $bookId is not in the collection!")
            return@start
        }

        // else
        val removeBookFromCollectionQuery = Database.connection.prepareStatement(
            """
                DELETE FROM contains
                WHERE collection_id = ? AND book_id = ? AND reader_id = ?
                RETURNING *
            """.trimIndent()
        )
        removeBookFromCollectionQuery.setInt(1, state.collection!!.id)
        removeBookFromCollectionQuery.setInt(2, bookId)
        removeBookFromCollectionQuery.setInt(3, state.user!!.id)
        val (queryStatus, _) = Database.runQuery(removeBookFromCollectionQuery, Contains::class)
        if (queryStatus != Database.QueryStatus.SUCCESS) {
            println("Failed removing book with id $bookId from collection with id ${state.collection!!.id}")
            return@start
        }

        println("Successfully removed book with id $bookId from collection with id ${state.collection!!.id}")

    }

    val deleteCollection: CommandCallback = start@ { state, _ ->
        // prelim checks
        if (!defaultChecks(state, "delete a")) { return@start }

        val deleteCollectionStatement = Database.connection.prepareStatement(
            """
                DELETE FROM collection
                WHERE collection_id = ? AND reader_id = ?
                RETURNING *
            """.trimIndent()
        )
        deleteCollectionStatement.setInt(1, state.collection!!.id)
        deleteCollectionStatement.setInt(2, state.user!!.id)
        val (queryStatus, _) = Database.runQuery(deleteCollectionStatement, Collection::class)
        if (queryStatus != Database.QueryStatus.SUCCESS) {
            println("Unable to delete collection with id ${state.collection!!.id}!")
            return@start
        }

        println("Successfully deleted collection with id ${state.collection!!.id}!")
        state.collection = null
    }

    val exitCollection: CommandCallback = start@ { state, _ ->
        if (state.collection == null) {
            println("You need to be in a collection to exit it!")
            return@start
        }

        println("Exited collection with id ${state.collection!!.id}")
        state.collection = null
    }

    private fun defaultChecks(state: State, action: String): Boolean {
        var result = true

        if (state.collection == null) {
            println("You need to be in a collection to $action it!")
            result = false
        } else if (state.user == null) {
            println("You need to be logged in to $action a collection!")
            result = false
        } else if (state.collection?.owner != state.user?.id) {
            println("Only the owner can $action the collection!")
            result = false
        }

        return result
    }

    private fun getInput(prompt: String, length: Int): String? {
        print(prompt)
        val result = readln()
        if (result.isEmpty() || result.length > length) { return null }

        return result
    }
}