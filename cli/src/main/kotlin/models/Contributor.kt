package models

import annotations.Column
import annotations.Entity
import java.time.Instant

@Entity("contributor")
interface Contributor {
    @Column("contributor_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("contributorContributorId")
    @set:JvmName("setContributorContributorId")
    var id: Int

    @Column("dob")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("contributorDOB")
    @set:JvmName("setContributorDOB")
    var dob: java.util.Date

    @Column("name")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("contributorName")
    @set:JvmName("setContributorName")
    var name: String
}
