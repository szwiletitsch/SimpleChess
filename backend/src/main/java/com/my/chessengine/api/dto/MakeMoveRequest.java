package com.my.chessengine.api.dto;

public record MakeMoveRequest(
        String fen,
        String from,
        String to
) {
}
