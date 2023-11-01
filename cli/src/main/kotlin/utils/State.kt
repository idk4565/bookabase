package utils

import models.Book
import models.Collection
import models.Reader

interface State {
    var user: Reader?
    var collection: Collection?
    var book: Book?
}