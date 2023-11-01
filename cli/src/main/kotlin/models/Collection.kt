package models

import annotations.Column
import annotations.Entity

@Entity("collection")
interface Collection {
    @Column("collection_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("collectionBookId")
    @set:JvmName("setCollectionBookId")
    var id: Int

    @Column("collection_name")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("collectionName")
    @set:JvmName("setCollectionName")
    var name: String

    @Column("reader_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("collectionOwner")
    @set:JvmName("setCollectionOwner")
    var owner: Int
}
