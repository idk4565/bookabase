package models

import annotations.Column
import annotations.Entity

@Entity("publishes")
interface Publishes {
    @Column("contributor_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("publishesContributorId")
    @set:JvmName("setPublishesContributorId")
    var contributorId: Int

    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("publishesBookId")
    @set:JvmName("setPublishesBookId")
    var bookId: Int
}
