package com.ddoong2.direnvloader

// direnv 실행 관련 예외
open class DirenvException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
}
