import utils.State

typealias ArgumentValidator = (String) -> Boolean
typealias CommandCallback = (State, List<String>) -> Unit

class Command(
    val domain: String = "",
    val name: String,
    val arguments: List<ArgumentValidator>,
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
        enter [email]
            search [email]
            follow [email]
            unfollow [email]
            TODO: collections
            TODO: following
            TODO: followers
            TODO: top10
            TODO: recommend [recent | followers | month | for_me]
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
        search [rel_date | authors | publishers | length | audience | rating] [value] sort=[rel_date | authors | publishers | length | audience | rating] [asc | desc]
        enter [id | random] 
          rate [rating]
          read [start_page] [end_page] [start_time] [end_time]
          stop
          exit
    """

    fun parseCommand(input: String) {
        var key = input.substring(0, input.indexOfFirst { it == ' ' })
        var mutableInput = input.substring(input.indexOfFirst { it == ' ' } + 1)
        if (!commands.containsKey(key)) {
            var rest = mutableInput.indexOfFirst { it == ' ' }
            if (rest == -1) { rest = mutableInput.length }
            key = "${key}.${mutableInput.substring(0, rest)}"
            mutableInput = mutableInput.substring(if (rest == mutableInput.length) rest else rest + 1)

            if (!commands.containsKey(key)) {
                println("Command does not exist!")
                return
            }
        }

        val command = commands[key]!!
        val arguments = mutableListOf<String>()
        var start = 0; var current = 0; var inQuote = false
        while (current < mutableInput.length) {
            if (mutableInput[current] == '"') {
                if (inQuote) {
                    arguments.add(mutableInput.substring(start + 1, current))
                    start = current + 1
                    inQuote = false
                } else {
                    inQuote = true
                }
            } else if (mutableInput[current] == ' ' && !inQuote) {
                arguments.add(mutableInput.substring(start, current))
                start = current + 1
            }

            ++current
        }
        if (start != current) arguments.add(mutableInput.substring(start, current))

        // Some basic checks so it doesn't explode
        if (arguments.size != command.arguments.size) {
            println("Too little to too many arguments passed! " +
                    "Got ${arguments.size}, Expected ${command.arguments.size}")
            return
        }

        // validate each item
        for (i in arguments.indices) {
            if (!command.arguments[i].invoke(arguments[i])) {
                println("Argument '${arguments[i]}' failed validation!")
                return
            }
        }

        command.callback(state, arguments)
    }
}