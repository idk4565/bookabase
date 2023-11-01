package models

import annotations.Column
import annotations.Entity

@Entity("follows")
interface Follows {
    @Column("follower_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("followsFollowerId")
    @set:JvmName("setFollowsFollowerId")
    var followerId: Int

    @Column("followee_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("followsFolloweeId")
    @set:JvmName("setFollowsFolloweeId")
    var followeeId: Int
}
