package GameFlow.Database

/** Wraps persistence failures so callers can log & pause rather than crash. */
class DatabaseException : RuntimeException {
    constructor(message: String, cause: Throwable?) : super(message, cause)
    constructor(message: String) : super(message)
}