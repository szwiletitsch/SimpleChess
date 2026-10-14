package com.my.chessengine.api.dto;

public record AnalysisRequest(
        String fen,
        int depth
) {
}
