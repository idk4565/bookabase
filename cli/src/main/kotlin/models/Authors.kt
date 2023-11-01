package models

import annotations.Column
import annotations.Entity

@Entity("authors")
interface Authors {
    @Column("contributor_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("authorsContributorId")
    @set:JvmName("setAuthorsContributorId")
    var contributorId: Int

    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("authorsBookId")
    @set:JvmName("setAuthorsBookId")
    var bookId: Int
}
