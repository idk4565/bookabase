import utils.State
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

//InternetAddress

typealias ArgumentValidator = (Any) -> Boolean
typealias CommandCallback = (State, List<Any>) -> Unit

class Command(
    val domain: String = "",
    val name: String,
    val arguments: List<Pair<KClass<*>, ArgumentValidator>>,
    val callback: CommandCallback
)

class CommandManager(var state: State, vararg commands: Command) {
    private val commands: Map<String, Command> = commands.associateBy {
        if (it.domain.isNotEmpty()) {
            "${it.domain}.${it.name}"
        } else {
            it.name
        }
    }

    private val helpMessage: String = """
    Available Commands:
      user
        list
        enter [name]
          follow [email]
          unfollow [email]
        exit
      collection
        list
        enter [name]
          update [new_name]
          add [book_id]
          remove [book_id]
          delete
        exit
      book
        list [rel_date | authors | publishers | length | audience | rating]=value sort=[rel_date | authors | publishers | length | audience | rating] asc | desc
        enter [id | random] 
          rate [rating]
          read [start_page] [end_page] [start_time] [end_time]
        exit
    """

    fun parseCommand(input: String) {
        val splitInput: MutableList<String> = input.split(" ").toMutableList()
        var key = splitInput.removeFirst()
        if (splitInput.size >= 1 && !commands.containsKey(key)) { key = "${key}.${splitInput.removeFirst()}" }
        if (!commands.containsKey(key)) { return }
        val command = commands[key]!!

        // Some basic checks so it doesn't explode
        if (splitInput.size < command.arguments.size) { return }
        else if (splitInput.size > command.arguments.size) { return }

        val results = mutableListOf<Any>()
        for ((i, v) in splitInput.withIndex()) {
            val argumentInfo = command.arguments[i]
            if (argumentInfo.first.safeCast(v) == null) { return }
            if (!argumentInfo.second(v)) { return }

            results.add(v)
        }

        command.callback(state, results)
    }
}