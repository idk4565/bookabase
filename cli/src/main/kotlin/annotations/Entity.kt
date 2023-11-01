package annotations

@Retention(AnnotationRetention.RUNTIME)
annotation class Entity(val table: String);
