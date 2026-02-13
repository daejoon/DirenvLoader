package com.github.direnvloader;

// direnv 실행 관련 예외
public class DirenvException extends Exception {
    public DirenvException(String message) {
        super(message);
    }

    public DirenvException(String message, Throwable cause) {
        super(message, cause);
    }
}
