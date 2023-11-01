package models

import annotations.Column
import annotations.Entity

@Entity("audience")
interface Audience {
    @Column("audience_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("audienceAudienceId")
    @set:JvmName("setAudienceAudienceId")
    var id: Int

    @Column("audience_name")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("audienceAudienceName")
    @set:JvmName("setAudienceAudienceName")
    var name: String
}
