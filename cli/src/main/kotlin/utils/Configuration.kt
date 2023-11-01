package utils

object Configuration {
    val lport: Int = (System.getenv("lport") ?: "5432").toInt();
    val rport: Int = (System.getenv("rport") ?: "5432").toInt();
    val rhost: String = System.getenv("rhost") ?: "starbug.cs.rit.edu";

    val user: String = System.getenv("user") ?: "";
    val password: String = System.getenv("password") ?: "";
    val databaseName: String = System.getenv("databaseName") ?: "";
}