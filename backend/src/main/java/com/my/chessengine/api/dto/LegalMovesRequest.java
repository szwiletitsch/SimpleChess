package com.my.chessengine.api.dto;

public record LegalMovesRequest(
        String fen,
        String square // something like "e4", "a7" etc
) {
}
