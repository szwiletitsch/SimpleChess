package com.my.chessengine.engine;

import com.my.chessengine.api.dto.GameStatus;

import java.util.*;

public class Game {
    public static final int[][] KNIGHT_OFFSETS = {{-2, 1}, {-2, -1}, {-1, 2}, {-1, -2}, {1, 2}, {1, -2}, {2, 1}, {2, -1}};
    public static final int[][] BISHOP_OFFSETS = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
    public static final int[][] ROOK_OFFSETS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    public static final int[][] QUEEN_KING_OFFSETS = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    protected long whitePawns, blackPawns, whiteKings, blackKings, whiteBishops, blackBishops, whiteQueens, blackQueens, whiteKnights, blackKnights, whiteRooks, blackRooks;
    protected boolean castlingRightsWhiteQueen, castlingRightsBlackQueen, castlingRightsWhiteKing, castlingRightsBlackKing;
    protected boolean isWhitesTurn;
    protected long enPassantSquare; // 0 if absent, otherwise if double push was performed last move it contains the skipped square
    protected int fullMoveNumber;
    protected int halfMoveClock;

    public Game(String fen) {
        loadFen(fen);
    }

    // ************************************************
    // * FEN LOADING                                  *
    // ************************************************
    public void loadFen(String fen) {
        clearPosition();

        String[] fields = fen.trim().split("\\s+");

        if (fields.length != 6) {
            throw new IllegalArgumentException("Invalid FEN: expected 6 fields");
        }
        parsePiecePlacement(fields[0]);
        parseActiveColor(fields[1]);
        parseCastlingRights(fields[2]);
        parseEnPassantSquare(fields[3]);
        halfMoveClock = Integer.parseInt(fields[4]);
        fullMoveNumber = Integer.parseInt(fields[5]);
    }

    private void clearPosition() {
        whitePawns = 0;
        blackPawns = 0;
        whiteKings = 0;
        blackKings = 0;
        whiteBishops = 0;
        blackBishops = 0;
        whiteQueens = 0;
        blackQueens = 0;
        whiteKnights = 0;
        blackKnights = 0;
        whiteRooks = 0;
        blackRooks = 0;
        isWhitesTurn = false;
        enPassantSquare = 0;
        fullMoveNumber = 1;
        halfMoveClock = 0;
        castlingRightsWhiteQueen = false;
        castlingRightsBlackQueen = false;
        castlingRightsWhiteKing = false;
        castlingRightsBlackKing = false;
    }

    private void parsePiecePlacement(String pieces) {
        String[] ranks = pieces.trim().split("/");

        if (ranks.length != 8) {
            throw new IllegalArgumentException("Invalid FEN: expected 8 ranks, but got: " + ranks.length);
        }

        for (int i = 0; i < 8; i++) {
            int r = 7 - i;
            int f = 0;
            for (char c : ranks[i].toCharArray()) {
                int offset = 1;
                long currentSquare = 1L << (r * 8 + f);
                switch (c) {
                    case '1', '2', '3', '4', '5', '6', '7', '8' -> {
                        offset = c - '0';
                    }
                    case 'p' -> {
                        blackPawns |= currentSquare;
                    }
                    case 'r' -> {
                        blackRooks |= currentSquare;
                    }
                    case 'n' -> {
                        blackKnights |= currentSquare;
                    }
                    case 'b' -> {
                        blackBishops |= currentSquare;
                    }
                    case 'q' -> {
                        blackQueens |= currentSquare;
                    }
                    case 'k' -> {
                        blackKings |= currentSquare;
                    }
                    case 'P' -> {
                        whitePawns |= currentSquare;
                    }
                    case 'R' -> {
                        whiteRooks |= currentSquare;
                    }
                    case 'N' -> {
                        whiteKnights |= currentSquare;
                    }
                    case 'B' -> {
                        whiteBishops |= currentSquare;
                    }
                    case 'Q' -> {
                        whiteQueens |= currentSquare;
                    }
                    case 'K' -> {
                        whiteKings |= currentSquare;
                    }
                    default -> {
                        throw new IllegalArgumentException("Invalid FEN: unknown piece character: " + c);
                    }
                }
                f += offset;
            }
            if (f != 8) {
                throw new IllegalArgumentException("Invalid FEN: expected 8 files on each rank, but got: " + f + " on rank: " + r + " (" + ranks[r] + ")");
            }
        }
    }

    private void parseActiveColor(String activeColor) {
        if (activeColor.equals("w")) {
            isWhitesTurn = true;
        } else if (activeColor.equals("b")) {
            isWhitesTurn = false;
        } else {
            throw new IllegalArgumentException("Invalid FEN: active color is supposed to be either 'w' or 'b' but got: " + activeColor);
        }
    }

    private void parseCastlingRights(String castlingRights) {
        if (castlingRights.equals("-")) {
            return;
        }
        for (char c : castlingRights.toCharArray()) {
            switch (c) {
                case 'Q' -> {
                    castlingRightsWhiteQueen = true;
                }
                case 'K' -> {
                    castlingRightsWhiteKing = true;
                }
                case 'q' -> {
                    castlingRightsBlackQueen = true;
                }
                case 'k' -> {
                    castlingRightsBlackKing = true;
                }
                default -> {
                    throw new IllegalArgumentException("Invalid FEN: expected any of 'q', 'k', 'Q', or 'K' but got: " + c);
                }
            }
        }
    }

    private void parseEnPassantSquare(String enPassantSquare) {
        if (enPassantSquare.equals("-")) {
            return;
        }
        if (enPassantSquare.length() != 2) {
            throw new IllegalArgumentException("Invalid FEN: Malformed En Passant Square Notation: " + enPassantSquare);
        }
        int file = enPassantSquare.charAt(0) - 'a';
        int rank = enPassantSquare.charAt(1) - '1';

        if (rank >= 8 || rank < 0 || file < 0 || file >= 8) {
            throw new IllegalArgumentException("Invalid FEN: Invalid En Passant Square: " + enPassantSquare);
        }

        this.enPassantSquare = 1L << (rank * 8 + file);
    }


    // ************************************************
    // * FEN EXPORT                                   *
    // ************************************************

    public String toFen() {
        return exportPiecePlacement() + " "
                + (isWhitesTurn ? "w" : "b") + " "
                + exportCastlingRights() + " "
                + exportEnPassantSquare() + " "
                + halfMoveClock + " "
                + fullMoveNumber;
    }

    private String exportPiecePlacement() {
        StringBuilder fen = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            int emptySquares = 0;

            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                long mask = 1L << square;

                char piece = getPieceAt(mask);

                if (piece == '\0') {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        fen.append(emptySquares);
                        emptySquares = 0;
                    }
                    fen.append(piece);
                }
            }

            if (emptySquares > 0) {
                fen.append(emptySquares);
            }

            if (rank > 0) {
                fen.append('/');
            }
        }

        return fen.toString();
    }

    private char getPieceAt(long square) {
        if ((whitePawns & square) != 0)   return 'P';
        if ((whiteKnights & square) != 0) return 'N';
        if ((whiteBishops & square) != 0) return 'B';
        if ((whiteRooks & square) != 0)   return 'R';
        if ((whiteQueens & square) != 0)  return 'Q';
        if ((whiteKings & square) != 0)   return 'K';

        if ((blackPawns & square) != 0)   return 'p';
        if ((blackKnights & square) != 0) return 'n';
        if ((blackBishops & square) != 0) return 'b';
        if ((blackRooks & square) != 0)   return 'r';
        if ((blackQueens & square) != 0)  return 'q';
        if ((blackKings & square) != 0)   return 'k';

        return '\0';
    }

    private String exportCastlingRights() {
        StringBuilder rights = new StringBuilder();

        if (castlingRightsWhiteKing)  rights.append('K');
        if (castlingRightsWhiteQueen) rights.append('Q');
        if (castlingRightsBlackKing)  rights.append('k');
        if (castlingRightsBlackQueen) rights.append('q');

        return rights.isEmpty() ? "-" : rights.toString();
    }

    private String exportEnPassantSquare() {
        if (enPassantSquare == 0) {
            return "-";
        }

        int square = Long.numberOfTrailingZeros(enPassantSquare);
        char file = (char) ('a' + square % 8);
        char rank = (char) ('1' + square / 8);

        return "" + file + rank;
    }

    // ************************************************
    // * HELPERS                                      *
    // ************************************************
    protected long whitePieces() {
        return whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKings;
    }

    protected long blackPieces() {
        return blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKings;
    }

    protected long occupied() {
        return whitePieces() | blackPieces();
    }

    public char getPiece(long piecePosition) {
        if ((whitePawns & piecePosition) != 0) {
            return 'P';
        }
        if ((whiteRooks & piecePosition) != 0) {
            return 'R';
        }
        if ((whiteKnights & piecePosition) != 0) {
            return 'N';
        }
        if ((whiteBishops & piecePosition) != 0) {
            return 'B';
        }
        if ((whiteQueens & piecePosition) != 0) {
            return 'Q';
        }
        if ((whiteKings & piecePosition) != 0) {
            return 'K';
        }
        if ((blackPawns & piecePosition) != 0) {
            return 'p';
        }
        if ((blackRooks & piecePosition) != 0) {
            return 'r';
        }
        if ((blackKnights & piecePosition) != 0) {
            return 'n';
        }
        if ((blackBishops & piecePosition) != 0) {
            return 'b';
        }
        if ((blackQueens & piecePosition) != 0) {
            return 'q';
        }
        if ((blackKings & piecePosition) != 0) {
            return 'k';
        }
        return '-';
    }

    private void removePieceAt(long square) {
        whitePawns &= ~square;
        blackPawns &= ~square;
        whiteKings &= ~square;
        blackKings &= ~square;
        whiteBishops &= ~square;
        blackBishops &= ~square;
        whiteQueens &= ~square;
        blackQueens &= ~square;
        whiteKnights &= ~square;
        blackKnights &= ~square;
        whiteRooks &= ~square;
        blackRooks &= ~square;
    }

    private void placePieceAt(long square, char piece) {
        switch (piece) {
            case 'P' -> whitePawns |= square;
            case 'p' -> blackPawns |= square;
            case 'K' -> whiteKings |= square;
            case 'k' -> blackKings |= square;
            case 'B' -> whiteBishops |= square;
            case 'b' -> blackBishops |= square;
            case 'Q' -> whiteQueens |= square;
            case 'q' -> blackQueens |= square;
            case 'N' -> whiteKnights |= square;
            case 'n' -> blackKnights |= square;
            case 'R' -> whiteRooks |= square;
            case 'r' -> blackRooks |= square;
            default -> throw new IllegalArgumentException("Unknown piece: " + piece);
        }
    }

    private void updateCastlingRightsForRookMove(long pos, char piece) {
        if (pos == 1L && piece == 'R') {
            castlingRightsWhiteQueen = false;
        } else if (pos == (1L << 7) && piece == 'R') {
            castlingRightsWhiteKing = false;
        } else if (pos == (1L << 56) && piece == 'r') {
            castlingRightsBlackQueen = false;
        } else if (pos == (1L << 63) && piece == 'r') {
            castlingRightsBlackKing = false;
        }
    }

    private boolean isOutOfBounds(int rank, int file) {
        return (rank >= 8 || file >= 8 || rank < 0 || file < 0);
    }


    // ************************************************
    // * LOGIC                                        *
    // ************************************************
    protected List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();

        generatePawnMoves(moves);
        generateKnightMoves(moves);
        generateBishopMoves(moves);
        generateRookMoves(moves);
        generateQueenMoves(moves);
        generateKingMoves(moves);

        return moves;
    }

    private void generateKingMoves(List<Move> moves) {
        long myKings;
        long myPieces;

        if (isWhitesTurn) {
            myKings = whiteKings;
            myPieces = whitePieces();
        } else {
            myKings = blackKings;
            myPieces = blackPieces();
        }

        int square = Long.numberOfTrailingZeros(myKings);
        int file = square % 8;
        int rank = square / 8;

        long fromPos = 1L << (rank * 8 + file);

        for (int[] offset : QUEEN_KING_OFFSETS) {
            int toRank = rank + offset[0];
            int toFile = file + offset[1];

            if (isOutOfBounds(toRank, toFile)) {
                continue;
            }

            long toPos = 1L << toRank * 8 + toFile;

            if ((toPos & myPieces) == 0) {
                moves.add(new Move(fromPos, toPos, null));
            }
        }

        if (isWhitesTurn) {
            if (castlingRightsWhiteKing) {
                tryCastle(moves, true, true);
            }
            if (castlingRightsWhiteQueen) {
                tryCastle(moves, true, false);
            }
        } else {
            if (castlingRightsBlackKing) {
                tryCastle(moves, false, true);
            }
            if (castlingRightsBlackQueen) {
                tryCastle(moves, false, false);
            }
        }
    }

    private void tryCastle(List<Move> moves, boolean white, boolean kingSide) {

        int rank = white ? 0 : 7;

        int kingFile = 4;
        int rookFile = kingSide ? 7 : 0;
        int destinationFile = kingSide ? 6 : 2;
        int transitFile = kingSide ? 5 : 3;

        long kingFrom = 1L << (rank * 8 + kingFile);
        long rookFrom = 1L << (rank * 8 + rookFile);
        long kingTo = 1L << (rank * 8 + destinationFile);

        long myKing = white ? whiteKings : blackKings;
        long myRooks = white ? whiteRooks : blackRooks;
        long opponentKing = white ? blackKings : whiteKings;

        // The king and rook must be on their original squares.
        if ((myKing & kingFrom) == 0 || (myRooks & rookFrom) == 0) {
            return;
        }

        // The king cannot castle out of check.
        if (isSquareAttacked(rank, kingFile, !white)) {
            return;
        }

        // Squares between king and rook must be empty.
        long occupied = occupied();

        if (kingSide) {
            int fSquare = rank * 8 + 5;
            int gSquare = rank * 8 + 6;

            if ((occupied & (1L << fSquare)) != 0 || (occupied & (1L << gSquare)) != 0) {
                return;
            }
        } else {
            int bSquare = rank * 8 + 1;
            int cSquare = rank * 8 + 2;
            int dSquare = rank * 8 + 3;

            if ((occupied & ((1L << bSquare) | (1L << cSquare) | (1L << dSquare))) != 0) {
                return;
            }
        }

        // The king cannot pass through check or end up in check.
        if (isSquareAttacked(rank, transitFile, !white) || isSquareAttacked(rank, destinationFile, !white)) {
            return;
        }

        moves.add(new Move(kingFrom, kingTo, null));
    }

    private void generateQueenMoves(List<Move> moves) {
        generateSlidingMoves(moves, isWhitesTurn ? whiteQueens : blackQueens, QUEEN_KING_OFFSETS);
    }

    private void generateRookMoves(List<Move> moves) {
        generateSlidingMoves(moves, isWhitesTurn ? whiteRooks : blackRooks, ROOK_OFFSETS);
    }

    private void generateBishopMoves(List<Move> moves) {
        generateSlidingMoves(moves, isWhitesTurn ? whiteBishops : blackBishops, BISHOP_OFFSETS);
    }

    private void generateSlidingMoves(List<Move> moves, long myMovingPieces, int[][] offsets) {
        long opponentPieces;
        long myPieces;

        if (isWhitesTurn) {
            opponentPieces = blackPieces();
            myPieces = whitePieces();
        } else {
            opponentPieces = whitePieces();
            myPieces = blackPieces();
        }

        while (myMovingPieces != 0) {
            int square = Long.numberOfTrailingZeros(myMovingPieces);
            int file = square % 8;
            int rank = square / 8;

            long fromPos = 1L << (rank * 8 + file);

            for (int[] offset : offsets) {
                int toRank = rank;
                int toFile = file;
                while (true) {
                    toRank += offset[0];
                    toFile += offset[1];

                    if (isOutOfBounds(toRank, toFile)) {
                        break;
                    }

                    long toPos = 1L << toRank * 8 + toFile;

                    if ((toPos & myPieces) != 0) {
                        break;
                    }

                    moves.add(new Move(fromPos, toPos, null));

                    if ((toPos & opponentPieces) != 0) {
                        break;
                    }
                }
            }

            myMovingPieces &= myMovingPieces - 1; // Remove the lowest set bit
        }
    }

    private void generateKnightMoves(List<Move> moves) {
        long myKnights;
        long myPieces;

        if (isWhitesTurn) {
            myKnights = whiteKnights;
            myPieces = whitePieces();
        } else {
            myKnights = blackKnights;
            myPieces = blackPieces();
        }

        while (myKnights != 0) {
            int square = Long.numberOfTrailingZeros(myKnights);
            int file = square % 8;
            int rank = square / 8;

            long fromPos = 1L << (rank * 8 + file);

            for (int[] offset : KNIGHT_OFFSETS) {
                int toRank = rank + offset[0];
                int toFile = file + offset[1];

                if (isOutOfBounds(toRank, toFile)) {
                    continue;
                }

                long toPos = 1L << toRank * 8 + toFile;

                if ((toPos & myPieces) == 0) {
                    moves.add(new Move(fromPos, toPos, null));
                }
            }

            myKnights &= myKnights - 1; // Remove the lowest set bit
        }
    }

    private void generatePawnMoves(List<Move> moves) {
        long myPawns;
        long opponentPieces;
        long occupied = occupied();
        int moveDirection;
        int push2RankRequirement;
        int promotionRank;
        if (isWhitesTurn) {
            myPawns = whitePawns;
            opponentPieces = blackPieces();
            moveDirection = 1;
            push2RankRequirement = 1;
            promotionRank = 7;
        } else {
            myPawns = blackPawns;
            opponentPieces = whitePieces();
            moveDirection = -1;
            push2RankRequirement = 6;
            promotionRank = 0;
        }

        while (myPawns != 0) {
            int square = Long.numberOfTrailingZeros(myPawns);
            int file = square % 8;
            int rank = square / 8;

            // A game state with a pawn on the promotion rank should be impossible, unless directly imported by a user
            // via fen string. In this case this Pawn will never have any possible moves.
            if (promotionRank == rank) {
                myPawns &= myPawns - 1; // Remove the lowest set bit
                continue;
            }

            long fromPos = 1L << (rank * 8 + file);
            // Move Logic:
            // 1) Normal Push:
            long pushPos = 1L << ((rank + moveDirection) * 8 + file);
            if ((occupied & pushPos) == 0) {
                addPawnMove(moves, fromPos, pushPos, promotionRank);
            }
            // 2) Initial 2-Square Push:
            long push2Pos = 1L << ((rank + moveDirection * 2) * 8 + file);
            if (push2RankRequirement == rank && (occupied & pushPos) == 0 && (occupied & push2Pos) == 0) {
                addPawnMove(moves, fromPos, push2Pos, promotionRank);
            }
            // 3) Normal Take:
            if (file != 0) {
                long takePosLeft = 1L << ((rank + moveDirection) * 8 + file - 1);
                if (((opponentPieces | enPassantSquare) & takePosLeft) != 0) {
                    addPawnMove(moves, fromPos, takePosLeft, promotionRank);
                }
            }
            if (file != 7) {
                long takePosRight = 1L << ((rank + moveDirection) * 8 + file + 1);
                if (((opponentPieces | enPassantSquare) & takePosRight) != 0) {
                    addPawnMove(moves, fromPos, takePosRight, promotionRank);
                }
            }

            myPawns &= myPawns - 1; // Remove the lowest set bit
        }
    }

    private void addPawnMove(List<Move> moves, long fromPos, long toPos, int promotionRank) {
        if (Long.numberOfTrailingZeros(toPos) / 8 == promotionRank) {
            moves.add(new Move(fromPos, toPos, PromotionPiece.QUEEN));
            moves.add(new Move(fromPos, toPos, PromotionPiece.ROOK));
            moves.add(new Move(fromPos, toPos, PromotionPiece.BISHOP));
            moves.add(new Move(fromPos, toPos, PromotionPiece.KNIGHT));
        } else {
            moves.add(new Move(fromPos, toPos, null));
        }
    }

    private boolean isSquareAttacked(int rank, int file, boolean byWhite) {

        long target = 1L << (rank * 8 + file);

        long pawns = byWhite ? whitePawns : blackPawns;
        long knights = byWhite ? whiteKnights : blackKnights;
        long bishops = byWhite ? whiteBishops : blackBishops;
        long rooks = byWhite ? whiteRooks : blackRooks;
        long queens = byWhite ? whiteQueens : blackQueens;
        long kings = byWhite ? whiteKings : blackKings;

        // 1) Pawn attacks
        // White pawns attack one rank higher; black pawns one rank lower.
        int pawnSourceRank = rank + (byWhite ? -1 : 1);

        if (pawnSourceRank >= 0 && pawnSourceRank < 8) {
            for (int fileOffset : new int[]{-1, 1}) {
                int pawnSourceFile = file + fileOffset;

                if (pawnSourceFile < 0 || pawnSourceFile >= 8) {
                    continue;
                }

                int pawnSquare = pawnSourceRank * 8 + pawnSourceFile;
                long pawnPos = 1L << pawnSquare;

                if ((pawns & pawnPos) != 0) {
                    return true;
                }
            }
        }

        // 2) Knight attacks
        for (int[] offset : KNIGHT_OFFSETS) {
            int r = rank + offset[0];
            int f = file + offset[1];

            if (isOutOfBounds(r, f)) {
                continue;
            }

            long pos = 1L << (r * 8 + f);

            if ((knights & pos) != 0) {
                return true;
            }
        }

        // 3) Bishop and queen attacks (diagonal rays)
        if (isAttackedBySlidingPiece(rank, file, BISHOP_OFFSETS, bishops | queens)) {
            return true;
        }

        // 4) Rook and queen attacks (straight rays)
        if (isAttackedBySlidingPiece(rank, file, ROOK_OFFSETS, rooks | queens)) {
            return true;
        }

        // 5) King attacks (one square in any direction)
        for (int[] offset : QUEEN_KING_OFFSETS) {
            int r = rank + offset[0];
            int f = file + offset[1];

            if (isOutOfBounds(r, f)) {
                continue;
            }

            long pos = 1L << (r * 8 + f);

            if ((kings & pos) != 0) {
                return true;
            }
        }

        return false;
    }

    private boolean isAttackedBySlidingPiece(int rank, int file, int[][] offsets, long attackingPieces) {

        for (int[] offset : offsets) {
            int r = rank;
            int f = file;

            while (true) {
                r += offset[0];
                f += offset[1];

                if (isOutOfBounds(r, f)) {
                    break;
                }

                long pos = 1L << (r * 8 + f);

                // The first occupied square blocks the ray.
                if ((pos & occupied()) != 0) {
                    if ((pos & attackingPieces) != 0) {
                        return true;
                    }

                    break;
                }
            }
        }

        return false;
    }

    protected boolean isKingInCheck(boolean whiteKing) {
        long king = whiteKing ? whiteKings : blackKings;

        if (king == 0) {
            throw new IllegalStateException("King is missing");
        }

        int kingSquare = Long.numberOfTrailingZeros(king);
        int file = kingSquare % 8;
        int rank = kingSquare / 8;

        return isSquareAttacked(rank, file, !whiteKing);
    }

    public List<Move> generateLegalMoves() {
        List<Move> legalMoves = new ArrayList<>();
        List<Move> pseudoLegalMoves = generatePseudoLegalMoves();

        boolean movingSideIsWhite = isWhitesTurn;

        for (Move move : pseudoLegalMoves) {
            UndoState undoState = makeMove(move);

            boolean legal = !isKingInCheck(movingSideIsWhite);

            undoMove(undoState);

            if (legal) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    public UndoState makeMove(Move move) {
        UndoState undoState = new UndoState(whitePawns, blackPawns, whiteKings, blackKings, whiteBishops, blackBishops, whiteQueens, blackQueens, whiteKnights, blackKnights, whiteRooks, blackRooks, castlingRightsWhiteQueen, castlingRightsBlackQueen, castlingRightsWhiteKing, castlingRightsBlackKing, isWhitesTurn, enPassantSquare, fullMoveNumber, halfMoveClock);

        long from = move.from();
        long to = move.to();

        char movingPiece = getPiece(from);

        if (movingPiece == '-') {
            throw new IllegalArgumentException("No piece on the source square");
        }

        boolean white = isWhitesTurn;
        boolean isPawn = movingPiece == 'P' || movingPiece == 'p';
        boolean isKing = movingPiece == 'K' || movingPiece == 'k';

        int fromSquare = Long.numberOfTrailingZeros(from);
        int toSquare = Long.numberOfTrailingZeros(to);

        // Determine the captured piece before modifying the board.
        char capturedPiece = getPiece(to);

        // Handle en passant capture.
        boolean isEnPassant = isPawn
                && to == enPassantSquare
                && capturedPiece == '-';

        if (isEnPassant) {
            long capturedPawnSquare = white ? to >>> 8 : to << 8;
            capturedPiece = getPiece(capturedPawnSquare);
            removePieceAt(capturedPawnSquare);
        }

        // Remove the moving piece from its original square.
        removePieceAt(from);

        // Remove the captured piece, if any.
        if (capturedPiece != '-' && !isEnPassant) {
            removePieceAt(to);
        }

        // Handle pawn promotion.
        char placedPiece = movingPiece;

        if (move.promotion() != null) {
            placedPiece = switch (move.promotion()) {
                case QUEEN -> white ? 'Q' : 'q';
                case ROOK -> white ? 'R' : 'r';
                case BISHOP -> white ? 'B' : 'b';
                case KNIGHT -> white ? 'N' : 'n';
                default -> throw new IllegalArgumentException("Invalid promotion");
            };
        }

        // Place the moving or promoted piece on the destination square.
        placePieceAt(to, placedPiece);

        // Handle castling by moving the rook.
        if (isKing && Math.abs(toSquare - fromSquare) == 2) {
            int rank = white ? 0 : 7;

            if (toSquare > fromSquare) {
                // King-side castling: h-file rook moves to f-file.
                long rookFrom = 1L << (rank * 8 + 7);
                long rookTo = 1L << (rank * 8 + 5);

                removePieceAt(rookFrom);
                placePieceAt(rookTo, white ? 'R' : 'r');
            } else {
                // Queen-side castling: a-file rook moves to d-file.
                long rookFrom = 1L << (rank * 8);
                long rookTo = 1L << (rank * 8 + 3);

                removePieceAt(rookFrom);
                placePieceAt(rookTo, white ? 'R' : 'r');
            }
        }

        // Update castling rights when a king moves.
        if (isKing) {
            if (white) {
                castlingRightsWhiteKing = false;
                castlingRightsWhiteQueen = false;
            } else {
                castlingRightsBlackKing = false;
                castlingRightsBlackQueen = false;
            }
        }

        // Update castling rights when a rook moves from its original square.
        updateCastlingRightsForRookMove(from, movingPiece);

        // Update castling rights if a rook is captured on its original square.
        if (capturedPiece != '-') {
            updateCastlingRightsForRookMove(to, capturedPiece);
        }

        // Reset en passant square by default.
        enPassantSquare = 0;

        // Set en passant square after a pawn double push.
        if (isPawn && Math.abs(toSquare - fromSquare) == 16) {
            enPassantSquare = white ? from << 8 : from >>> 8;
        }

        // Update the halfmove clock.
        if (isPawn || capturedPiece != '-') {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        // Increment fullmove number after Black's move.
        if (!white) {
            fullMoveNumber++;
        }

        // Switch the active player.
        isWhitesTurn = !isWhitesTurn;

        return undoState;
    }

    public GameStatus getGameStatus() {
        List<Move> moves = generateLegalMoves();
        boolean inCheck = isKingInCheck(isWhitesTurn);

        if (moves.isEmpty()) {
            return inCheck ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }

        return inCheck ? GameStatus.CHECK : GameStatus.IN_PROGRESS;
    }

    // ************************************************
    // * DEBUGGING todo remove / refactor             *
    // ************************************************
    private void assertValidPosition() {
        long[] bitmaps = {whitePawns, blackPawns, whiteRooks, blackRooks, whiteKnights, blackKnights, whiteBishops, whiteQueens, whiteKings, blackPawns, blackKings, blackBishops, blackQueens};
        for (int i = 0; i <= bitmaps.length - 1; i++) {
            for (int j = i + 1; j <= bitmaps.length - 1; j++) {
                if ((bitmaps[i] & bitmaps[j]) != 0) {
                    throw new IllegalStateException("Multiple Bitmaps have Pieces on the same square");
                }
            }
        }
    }

    public void printBoard() {
        System.out.println("\n  +------------------------+");

        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + " |");

            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                long mask = 1L << square;
                char piece = '.';

                if ((whitePawns & mask) != 0) piece = 'P';
                else if ((whiteKnights & mask) != 0) piece = 'N';
                else if ((whiteBishops & mask) != 0) piece = 'B';
                else if ((whiteRooks & mask) != 0) piece = 'R';
                else if ((whiteQueens & mask) != 0) piece = 'Q';
                else if ((whiteKings & mask) != 0) piece = 'K';
                else if ((blackPawns & mask) != 0) piece = 'p';
                else if ((blackKnights & mask) != 0) piece = 'n';
                else if ((blackBishops & mask) != 0) piece = 'b';
                else if ((blackRooks & mask) != 0) piece = 'r';
                else if ((blackQueens & mask) != 0) piece = 'q';
                else if ((blackKings & mask) != 0) piece = 'k';

                System.out.print(" " + piece + " ");
            }

            System.out.println("|");
        }

        System.out.println("  +------------------------+");
        System.out.println("    a  b  c  d  e  f  g  h");

        System.out.println("\n" + toFen());

        // Game state
        System.out.println("\n--- Game State ---");
        System.out.println("Side to move:       " + (isWhitesTurn ? "White" : "Black"));

        System.out.println("Castling rights:");
        System.out.println("  White: " + (castlingRightsWhiteKing ? "K" : "") + (castlingRightsWhiteQueen ? "Q" : ""));
        System.out.println("  Black: " + (castlingRightsBlackKing ? "k" : "") + (castlingRightsBlackQueen ? "q" : ""));

        System.out.println("En passant square:  " + enPassantSquareName());
        System.out.println("Halfmove clock:     " + halfMoveClock);
        System.out.println("Fullmove number:    " + fullMoveNumber);

        // Derived bitboards
        System.out.printf("White pieces:       0x%016X%n", whitePieces());
        System.out.printf("Black pieces:       0x%016X%n", blackPieces());
        System.out.printf("Occupied:           0x%016X%n", occupied());

        // Individual piece bitboards
        System.out.println("\n--- Piece Bitboards ---");
        System.out.printf("White pawns:        0x%016X%n", whitePawns);
        System.out.printf("White knights:      0x%016X%n", whiteKnights);
        System.out.printf("White bishops:      0x%016X%n", whiteBishops);
        System.out.printf("White rooks:        0x%016X%n", whiteRooks);
        System.out.printf("White queens:       0x%016X%n", whiteQueens);
        System.out.printf("White kings:        0x%016X%n", whiteKings);

        System.out.printf("Black pawns:        0x%016X%n", blackPawns);
        System.out.printf("Black knights:      0x%016X%n", blackKnights);
        System.out.printf("Black bishops:      0x%016X%n", blackBishops);
        System.out.printf("Black rooks:        0x%016X%n", blackRooks);
        System.out.printf("Black queens:       0x%016X%n", blackQueens);
        System.out.printf("Black kings:        0x%016X%n", blackKings);
    }

    private String enPassantSquareName() {
        if (enPassantSquare == 0) {
            return "-";
        }

        int square = Long.numberOfTrailingZeros(enPassantSquare);
        char file = (char) ('a' + square % 8);
        char rank = (char) ('1' + square / 8);

        return "" + file + rank;
    }

    // ************************************************
    // * Move Undoing                                 *
    // ************************************************
    public record UndoState(long whitePawns, long blackPawns, long whiteKings, long blackKings, long whiteBishops,
                             long blackBishops, long whiteQueens, long blackQueens, long whiteKnights,
                             long blackKnights, long whiteRooks, long blackRooks, boolean castlingRightsWhiteQueen,
                             boolean castlingRightsBlackQueen, boolean castlingRightsWhiteKing,
                             boolean castlingRightsBlackKing, boolean isWhitesTurn, long enPassantSquare,
                             int fullMoveNumber, int halfMoveClock) {
    }

    protected void undoMove(UndoState state) {
        whitePawns = state.whitePawns();
        blackPawns = state.blackPawns();
        whiteKings = state.whiteKings();
        blackKings = state.blackKings();
        whiteBishops = state.whiteBishops();
        blackBishops = state.blackBishops();
        whiteQueens = state.whiteQueens();
        blackQueens = state.blackQueens();
        whiteKnights = state.whiteKnights();
        blackKnights = state.blackKnights();
        whiteRooks = state.whiteRooks();
        blackRooks = state.blackRooks();

        castlingRightsWhiteQueen = state.castlingRightsWhiteQueen();
        castlingRightsBlackQueen = state.castlingRightsBlackQueen();
        castlingRightsWhiteKing = state.castlingRightsWhiteKing();
        castlingRightsBlackKing = state.castlingRightsBlackKing();

        isWhitesTurn = state.isWhitesTurn();
        enPassantSquare = state.enPassantSquare();
        fullMoveNumber = state.fullMoveNumber();
        halfMoveClock = state.halfMoveClock();
    }
}
