package com.ddoong2.direnvloader

// .envrc가 blocked 상태일 때 발생하는 예외
class DirenvBlockedException(message: String) : DirenvException(message)
