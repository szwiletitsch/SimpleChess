export type Color = 'white' | 'black'
export type PieceType = 'pawn' | 'rook' | 'knight' | 'bishop' | 'queen' | 'king'

export interface Piece {
    type: PieceType
    color: Color
}

export type Board = (Piece | null)[][]

export interface Square {
    row: number
    col: number
}

export interface CastlingRights {
    whiteKingSide: boolean
    whiteQueenSide: boolean
    blackKingSide: boolean
    blackQueenSide: boolean
}

export interface GameState {
    board: Board
    turn: Color
    castlingRights: CastlingRights
    moves: {from: Square, to: Square, piece: Piece}[]
    halfMoveClock: number
    fullMoveNumber: number
}