package com.github.direnvloader;

// .envrc가 blocked 상태일 때 발생하는 예외
public class DirenvBlockedException extends DirenvException {
    public DirenvBlockedException(String message) {
        super(message);
    }
}
