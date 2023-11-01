package utils

import annotations.Column
import annotations.Entity
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.postgresql.util.PSQLException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.Properties
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaGetter


object Database {
    private val session: Session
    val connection: Connection

    enum class QueryStatus {
        QUERY_FAILED,
        SUCCESS
    }

    class TypeCombinationFailure(message: String) : Throwable(message)
    class MissingModel(message: String) : Throwable(message)
    class MissingField(message: String) : Throwable(message)

    private val open: Boolean
        get() { return !connection.isClosed && session.isConnected }

    init {
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"

        // connect to JSch
        session = JSch().getSession(Configuration.user, Configuration.rhost, 22)
        session.setPassword(Configuration.password)
        session.setConfig(config)
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password")
        session.connect()

        // Connect to PostgresSQL
        val assignedPort: Int = session.setPortForwardingL(Configuration.lport, "127.0.0.1", Configuration.rport)
        val url = "jdbc:postgresql://127.0.0.1:${assignedPort}/${Configuration.databaseName}"
        val props = Properties()
        props["user"] = Configuration.user
        props["password"] = Configuration.password

        Class.forName("org.postgresql.Driver")
        connection = DriverManager.getConnection(url, props)
    }

    @Throws(TypeCombinationFailure::class)
    private fun combineModels(vararg models: KClass<*>): Any {
        // create the proxy otherwise
        val proxy = object : InvocationHandler {
            // deals with the stored fields
            private val fields: MutableMap<String, Any> = mutableMapOf()

            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any {
                if (method == null) { return Unit }
                val setter = (method.name.startsWith("set") && args?.size == 1)
                val methodName = if (setter) method.name.substring(3).replaceFirstChar(Char::lowercase) else method.name
                val key = "${method.declaringClass.name}.${methodName}"

                return if (setter) {
                    fields.set(key, args!!.first())
                } else {
                    fields.getOrPut(key) {
                        java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(method.returnType, 1), 0)
                    }
                }
            }
        }

        return Proxy.newProxyInstance(
            proxy::class.java.classLoader,
            models.map { it.java }.toTypedArray(),
            proxy
        )
    }

    @Throws(MissingModel::class, MissingField::class)
    fun runQuery(query: PreparedStatement, vararg models: KClass<*>): Pair<QueryStatus, List<Any>> {
        // we can only combine entities
        val nonEntity = models.find { !it.hasAnnotation<Entity>() }
        if (nonEntity != null) {
            throw TypeCombinationFailure("${nonEntity.simpleName} is not an Entity. All models must be annotated with @Entity.")
        }

        // execute query
        var queryResult: ResultSet? = null
        try {
            queryResult = query.executeQuery()
        } catch(e: PSQLException) { println(e.message) }
        if (queryResult == null) { return Pair(QueryStatus.QUERY_FAILED, listOf()) }

        // map results to classes
        val metaData = queryResult.metaData
        val tableToClass = models.associateBy { it.findAnnotation<Entity>()!!.table }
        val results = mutableListOf<Any>()

         while (queryResult.next()) {
             val row = combineModels(*models)
             for (i in 1..metaData.columnCount) {
                 val tableName = metaData.getTableName(i)
                 val columnName = metaData.getColumnName(i)
                 val relatedClass = tableToClass.getOrElse(tableName) { throw MissingModel("Missing model for table $tableName!") }
                 val fieldGetter = relatedClass.memberProperties.find {
                     it.hasAnnotation<Column>() && it.findAnnotation<Column>()?.name == columnName
                 }?.javaGetter ?: throw MissingField("Model is missing field for column $columnName!")
                 val fieldSetter = row::class.java.getMethod(
                     "set${fieldGetter.name.replaceFirstChar(Char::uppercase)}",
                     fieldGetter.returnType
                 )

                 fieldSetter.invoke(row, queryResult.getObject(i))
             }

             results.add(row)
         }

        return Pair(QueryStatus.SUCCESS, results.requireNoNulls().toList())
    }

    fun close() {
        if (!open) { return }

        connection.close()
        session.disconnect()
    }
}