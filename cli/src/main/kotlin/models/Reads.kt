package models

import annotations.Column
import annotations.Entity
import java.sql.Timestamp

@Entity("reads")
interface Reads {
    @Column("reader_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readsReaderId")
    @set:JvmName("setReadsReaderId")
    var readerId: Int

    @Column("book_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readsBookId")
    @set:JvmName("setReadsBookId")
    var bookId: Int

    @Column("start_time")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readsStartTime")
    @set:JvmName("setReadsStartTime")
    var startTime: Timestamp

    @Column("end_time")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readsEndTime")
    @set:JvmName("setReadsEndTime")
    var endTime: Timestamp

    @Column("start_page")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readsStartPage")
    @set:JvmName("setReadsStartPage")
    var startPage: Int

    @Column("end_page")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("readsEndPage")
    @set:JvmName("setReadsEndPage")
    var endPage: Int
}
