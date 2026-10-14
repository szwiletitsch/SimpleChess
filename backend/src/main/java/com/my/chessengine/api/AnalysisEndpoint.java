package com.my.chessengine.api;

import com.my.chessengine.api.dto.*;
import com.my.chessengine.service.ChessEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class AnalysisEndpoint {
    private final ChessEngineService service;

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody AnalysisRequest request) {
        return service.analyze(request);
    }

    @PostMapping("/legalMoves/")
    public LegalMovesResponse analyze(@RequestBody LegalMovesRequest request) {
        return service.calculateLegalMoves(request);
    }

    @PostMapping("/move")
    public MakeMoveResponse makeMove(
            @RequestBody MakeMoveRequest request
    ) {
        return service.makeMove(request);
    }
}
