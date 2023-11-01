package models

import annotations.Column
import annotations.Entity

@Entity("genre")
interface Genre {
    @Column("genre_id")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("genreGenreId")
    @set:JvmName("setGenreGenreId")
    var id: Int

    @Column("genre_name")
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("genreGenreName")
    @set:JvmName("setGenreGenreName")
    var name: String
}
