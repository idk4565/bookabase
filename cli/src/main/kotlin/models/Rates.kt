package models

import annotations.Column
import annotations.Entity

@Entity("rates")
interface Rates {
    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("ratesBookId")
    @set:JvmName("setRatesBookId")
    var bookId: Int

    @Column("reader_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("ratesReaderId")
    @set:JvmName("setRatesReaderId")
    var readerId: Int

    @Column("rating")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("ratesRating")
    @set:JvmName("setRatesRating")
    var rating: Int
}
