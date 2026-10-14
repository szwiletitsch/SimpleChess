package com.my.chessengine.api.dto;

import java.util.List;

public record LegalMovesResponse(
        List<String> allowedSquares // list of squares something like ["e4", "a7"]
) {
}
