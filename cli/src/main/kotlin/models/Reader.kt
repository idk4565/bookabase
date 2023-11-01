package models

import annotations.Column
import annotations.Entity

@Entity("reader")
interface Reader {
    @Column("reader_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerReaderId")
    @set:JvmName("setReaderReaderId")
    var id: Int

    @Column("username")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerUsername")
    @set:JvmName("setReaderUsername")
    var username: String

    @Column("firstname")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerFirstname")
    @set:JvmName("setReaderFirstname")
    var firstName: String

    @Column("lastname")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerLastname")
    @set:JvmName("setReaderLastname")
    var lastName: String

    @Column("password")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerPassword")
    @set:JvmName("setReaderPassword")
    var password: String

    @Column("email")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerEmail")
    @set:JvmName("setReaderEmail")
    var email: String

    @Column("last_access_date")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerLastAccessDate")
    @set:JvmName("setReaderLastAccessDate")
    var lastAccessDate: java.util.Date

    @Column("creation_date")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readerCreationDate")
    @set:JvmName("setReaderCreationDate")
    var creationDate: java.util.Date
}