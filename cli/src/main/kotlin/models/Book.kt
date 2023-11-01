package models

import annotations.Column
import annotations.Entity
import java.time.Instant

@Entity("book")
interface Book {
    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("bookBookId")
    @set:JvmName("setBookBookId")
    var id: Int

    @Column("title")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("bookTitle")
    @set:JvmName("setBookTitle")
    var title: String

    @Column("page_length")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("bookPageLength")
    @set:JvmName("setBookPageLength")
    var pageLength: Int

    @Column("release_date")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("bookReleaseDate")
    @set:JvmName("setBookReleaseDate")
    var releaseDate: java.util.Date

    @Column("genre_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("bookGenreId")
    @set:JvmName("setBookGenreId")
    var genreId: Int

    @Column("audience_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("bookAudienceId")
    @set:JvmName("setBookAudienceId")
    var audienceId: Int
}
