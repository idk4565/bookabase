package models

import annotations.Column
import annotations.Entity

@Entity("contains")
interface Contains {
    @Column("collection_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("containsCollectionId")
    @set:JvmName("setContainsCollectionId")
    var collectionId: Int

    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("containsBookId")
    @set:JvmName("setContainsBookId")
    var bookId: Int

    @Column("reader_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("containsReaderId")
    @set:JvmName("setContainsReaderId")
    var readerId: Int
}
