package com.my.chessengine.service;

import com.my.chessengine.api.Exceptions.ChessEngineIllegalArgument;
import com.my.chessengine.api.dto.*;

public interface ChessEngineService {
    AnalysisResponse analyze(AnalysisRequest analysisRequest) throws ChessEngineIllegalArgument;

    LegalMovesResponse calculateLegalMoves(LegalMovesRequest legalMovesRequest) throws ChessEngineIllegalArgument;

    MakeMoveResponse makeMove(MakeMoveRequest request) throws ChessEngineIllegalArgument;
}
