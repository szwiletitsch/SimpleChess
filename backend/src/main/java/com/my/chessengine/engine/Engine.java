package com.my.chessengine.engine;

import java.util.List;

public class Engine extends Game {
    public Engine(String fen) {
        super(fen);
    }

    // ************************************************
    // * MiniMax                                      *
    // ************************************************

    private double staticEvaluation() {
        // Material values in centi-pawns
        final double PAWN_VALUE = 100;
        final double KNIGHT_VALUE = 320;
        final double BISHOP_VALUE = 330;
        final double ROOK_VALUE = 500;
        final double QUEEN_VALUE = 900;

        double score = 0;

        // Material balance.
        score += Long.bitCount(whitePawns) * PAWN_VALUE;
        score -= Long.bitCount(blackPawns) * PAWN_VALUE;

        score += Long.bitCount(whiteKnights) * KNIGHT_VALUE;
        score -= Long.bitCount(blackKnights) * KNIGHT_VALUE;

        score += Long.bitCount(whiteBishops) * BISHOP_VALUE;
        score -= Long.bitCount(blackBishops) * BISHOP_VALUE;

        score += Long.bitCount(whiteRooks) * ROOK_VALUE;
        score -= Long.bitCount(blackRooks) * ROOK_VALUE;

        score += Long.bitCount(whiteQueens) * QUEEN_VALUE;
        score -= Long.bitCount(blackQueens) * QUEEN_VALUE;

        // Bishop pair bonus.
        if (Long.bitCount(whiteBishops) >= 2) {
            score += 30;
        }
        if (Long.bitCount(blackBishops) >= 2) {
            score -= 30;
        }

        // Mobility bonus: reward the side with more pseudo-legal moves.
        boolean originalTurn = isWhitesTurn;


        /* todo decide if mobility is worth the compute (it does not seem to be since it reduces depth searched in reasonable time from depth 8 to depth 6)
        int whiteMobility;
        int blackMobility;

        isWhitesTurn = true;
        whiteMobility = generatePseudoLegalMoves().size();

        isWhitesTurn = false;
        blackMobility = generatePseudoLegalMoves().size();

        isWhitesTurn = originalTurn;

        score += 2.0 * (whiteMobility - blackMobility);

         */

        // Small bonus for having the move.
        score += isWhitesTurn ? 10 : -10;

        return score;
    }

    public Move findBestMove(int maxDepth) {
        Move bestMove = null;

        for (int depth = 1; depth <= maxDepth; depth++) {
            Move move = findBestMoveAtDepth(depth, bestMove);

            if (move != null) {
                bestMove = move;
            }
        }

        return bestMove;
    }

    private Move findBestMoveAtDepth(int depth, Move previousBestMove) {
        List<Move> moves = generateLegalMoves();

        if (moves.isEmpty()) {
            return null;
        }

        orderMoves(moves);

        // Search the previous iteration's best move first.
        if (previousBestMove != null) {
            moves.remove(previousBestMove);
            moves.addFirst(previousBestMove);
        }

        Move bestMove = null;
        double bestScore = isWhitesTurn
                ? Double.NEGATIVE_INFINITY
                : Double.POSITIVE_INFINITY;

        for (Move move : moves) {
            Game.UndoState undoState = makeMove(move);

            double score = minimax(
                    depth - 1,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY
            );

            undoMove(undoState);

            if (bestMove == null
                    || (isWhitesTurn && score > bestScore)
                    || (!isWhitesTurn && score < bestScore)) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private double minimax(int depth, double alpha, double beta) {
        if (depth == 0) {
            return staticEvaluation();
        }

        List<Move> moves = generateLegalMoves();
        orderMoves(moves);

        if (moves.isEmpty()) {
            if (isKingInCheck(isWhitesTurn)) {
                // Checkmate: the side to move has lost.
                return isWhitesTurn ? -100000 : 100000;
            }

            // Stalemate.
            return 0;
        }

        if (isWhitesTurn) {
            // White maximizes the evaluation.
            double maxScore = Double.NEGATIVE_INFINITY;

            for (Move move : moves) {
                Game.UndoState undoState = makeMove(move);

                double score = minimax(depth - 1, alpha, beta);

                undoMove(undoState);

                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);

                if (beta <= alpha) {
                    break; // Alpha-beta cutoff.
                }
            }

            return maxScore;
        } else {
            // Black minimizes the evaluation.
            double minScore = Double.POSITIVE_INFINITY;

            for (Move move : moves) {
                Game.UndoState undoState = makeMove(move);

                double score = minimax(depth - 1, alpha, beta);

                undoMove(undoState);

                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);

                if (beta <= alpha) {
                    break; // Alpha-beta cutoff.
                }
            }

            return minScore;
        }
    }

    private int moveOrderingScore(Move move) {
        int score = 0;

        char attacker = getPiece(move.from());
        char victim = getPiece(move.to());

        // En passant captures land on an empty square.
        if ((attacker == 'P' || attacker == 'p')
                && move.to() == enPassantSquare
                && victim == '-') {
            victim = isWhitesTurn ? 'p' : 'P';
        }

        // MVV-LVA: prefer capturing valuable pieces
        // with less valuable attackers.
        if (victim != '-') {
            score += 10_000
                    + pieceValue(victim) * 10
                    - pieceValue(attacker);
        }

        // Promotions are generally strong moves.
        if (move.promotion() != null) {
            score += 8_000 + pieceValue(
                    switch (move.promotion()) {
                        case QUEEN -> isWhitesTurn ? 'Q' : 'q';
                        case ROOK -> isWhitesTurn ? 'R' : 'r';
                        case BISHOP -> isWhitesTurn ? 'B' : 'b';
                        case KNIGHT -> isWhitesTurn ? 'N' : 'n';
                    }
            );
        }

        return score;
    }

    private int pieceValue(char piece) {
        return switch (Character.toLowerCase(piece)) {
            case 'p' -> 100;
            case 'n' -> 320;
            case 'b' -> 330;
            case 'r' -> 500;
            case 'q' -> 900;
            case 'k' -> 20_000;
            default -> 0;
        };
    }

    private void orderMoves(List<Move> moves) {
        moves.sort((a, b) ->
                Integer.compare(
                        moveOrderingScore(b),
                        moveOrderingScore(a)
                )
        );
    }
}
