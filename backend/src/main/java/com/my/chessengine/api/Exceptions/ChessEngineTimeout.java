package com.my.chessengine.api.Exceptions;

public class ChessEngineTimeout extends RuntimeException {
    public ChessEngineTimeout(String message) {
        super(message);
    }
}
