package com.my.chessengine.api.dto;

public record AnalysisResponse(
        String bestMove,
        String fen,
        double eval,
        int depth,
        GameStatus status
) {
}
