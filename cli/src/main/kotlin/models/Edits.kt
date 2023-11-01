package models

import annotations.Column
import annotations.Entity

@Entity("edits")
interface Edits {
    @Column("contributor_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("editsContributorId")
    @set:JvmName("setEditsContributorId")
    var contributorId: Int

    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("editsBookId")
    @set:JvmName("setEditsBookId")
    var bookId: Int
}
