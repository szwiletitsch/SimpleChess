package com.my.chessengine.engine;

public record Move(long from, long to, PromotionPiece promotion) {
    @Override
    public String toString() {
        return posToNotation(from) + "->" + posToNotation(to) + (promotion != null ? " (" + promotion + ")" : "");
    }

    public static String posToNotation(long pos) {
        int square = Long.numberOfTrailingZeros(pos);
        int rank = square / 8;
        int file = square % 8;
        return ((char)('a' + file)) + "" + (rank + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Move move) {
            return move.from == from && move.to == to && move.promotion == promotion;
        }
        return false;
    }
}
