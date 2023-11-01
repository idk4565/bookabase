package models

import annotations.Column
import annotations.Entity

@Entity("")
interface Computed {
    @Column("computed1")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("computedComputed1")
    @set:JvmName("setComputedComputed1")
    var computed1: Int

    @Column("computed2")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("computedComputed2")
    @set:JvmName("setComputedComputed2")
    var computed2: Int

    @Column("computed3")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("computedComputed3")
    @set:JvmName("setComputedComputed3")
    var computed3: String

    @Column("computed4")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("computedComputed4")
    @set:JvmName("setComputedComputed4")
    var computed4: String

    @Column("computed5")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("computedComputed5")
    @set:JvmName("setComputedComputed5")
    var computed5: java.sql.Date

    @Column("computed6")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("computedComputed6")
    @set:JvmName("setComputedComputed6")
    var computed6: java.sql.Date
}