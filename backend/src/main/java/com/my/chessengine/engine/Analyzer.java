package com.my.chessengine.engine;


public class Analyzer {
    public static void main(String[] args) {
        String fen = "7k/4K1pp/7N/8/8/8/8/B7 w - - 0 1" ;
        Engine engine = new Engine(
                fen
        );

        Move bestMove = engine.findBestMove(8);
        engine.printBoard();

        System.out.println("Best move: " + bestMove);
    }
}
