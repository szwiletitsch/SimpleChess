package com.my.chessengine.service.impl;

import com.my.chessengine.api.Exceptions.ChessEngineIllegalArgument;
import com.my.chessengine.api.Exceptions.ChessEngineTimeout;
import com.my.chessengine.api.dto.*;
import com.my.chessengine.engine.Engine;
import com.my.chessengine.engine.Game;
import com.my.chessengine.engine.Move;
import com.my.chessengine.engine.PromotionPiece;
import com.my.chessengine.service.ChessEngineService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ChessEngineServiceImpl implements ChessEngineService {
    @Override
    public AnalysisResponse analyze(AnalysisRequest analysisRequest) {
        CompletableFuture<AnalysisResponse> task = CompletableFuture.supplyAsync(() -> {
            try {
                Engine engine = new Engine(analysisRequest.fen());
                engine.printBoard();
                Move move = engine.findBestMove(analysisRequest.depth());

                engine.makeMove(move);
                engine.printBoard();


                return new AnalysisResponse(move.toString(), engine.toFen(), 0d, analysisRequest.depth());
            } catch (IllegalArgumentException e) {
                throw new ChessEngineIllegalArgument(e.getMessage());
            }
        });

        try {
            return task.get(20, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            task.cancel(true);
            throw new ChessEngineTimeout("Engine took too long (more than 20 seconds)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChessEngineTimeout("Engine analysis was interrupted");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ChessEngineIllegalArgument chessException) {
                throw chessException;
            }

            throw new RuntimeException("Engine analysis failed", cause);
        }
    }

    @Override
    public LegalMovesResponse calculateLegalMoves(LegalMovesRequest legalMovesRequest) throws ChessEngineIllegalArgument {
        long square = notationToBitmap(legalMovesRequest.square());

        try {
            Game game = new Game(legalMovesRequest.fen());

            List<Move> movesFromSquare = game.generateLegalMoves().stream().filter(move -> move.from() == square).toList();

            return new LegalMovesResponse(
                    movesFromSquare.stream().map(Move::to).distinct().map(Move::posToNotation).toList()
            );
        } catch (IllegalArgumentException e) {
            throw new ChessEngineIllegalArgument(e.getMessage());
        }
    }

    @Override
    public MakeMoveResponse makeMove(MakeMoveRequest request) throws ChessEngineIllegalArgument {
        long from = notationToBitmap(request.from());
        long to = notationToBitmap(request.to());

        try {
            Game game = new Game(request.fen());

            Move moveToMake = new Move(
                    from,
                    to,
                    game.getPiece(from) == 'p' && request.to().charAt(1) == '1' ||
                            game.getPiece(from) == 'P' && request.to().charAt(1) == '8' ? PromotionPiece.QUEEN : null
            );

            System.out.println(from);

            System.out.println(moveToMake);
            for (Move move : game.generateLegalMoves()) {
                System.out.println(move);
            }

            if (game.generateLegalMoves().stream().noneMatch(moveToMake::equals)) {
                throw new ChessEngineIllegalArgument("Invalid move in current game state");
            }

            game.makeMove(moveToMake);

            return new MakeMoveResponse(
                    game.toFen()
            );
        } catch (IllegalArgumentException e) {
            throw new ChessEngineIllegalArgument(e.getMessage());
        }
    }

    private long notationToBitmap(String notation) throws ChessEngineIllegalArgument {
        if (notation == null || notation.length() != 2) {
            throw new ChessEngineIllegalArgument("Positions must be valid squares, e.g. e2 or e4. Instead got: " + notation);
        }

        int rank = notation.charAt(1) - '1';
        int file = notation.charAt(0) - 'a';

        if (rank > 7 || file > 7 || rank < 0 || file < 0 ) {
            throw new ChessEngineIllegalArgument("Positions must be valid squares, e.g. e2 or e4. Instead got: " + notation);
        }

        return  1L << (rank * 8 + file);
    }
}
