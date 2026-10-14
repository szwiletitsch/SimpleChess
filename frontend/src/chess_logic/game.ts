import type {Board, Square, GameState, Piece} from './types'
import {createInitialBoard} from "@/chess_logic/board.ts";

export function createInitialGameState(): GameState {
    return {
        board: createInitialBoard(),
        turn: 'white',
        castlingRights: {
            whiteKingSide: true,
            whiteQueenSide: true,
            blackKingSide: true,
            blackQueenSide: true,
        },
        moves: [],
        halfMoveClock: 0,
        fullMoveNumber: 1,
    }
}


export function movePiece(
    game: GameState,
    from: Square,
    to: Square,
) {
    const board = game.board

    const piece = board[from.row]![from.col]
    const capturedPiece = board[to.row]![to.col]

    if (!piece) {
        return
    }

    // Move piece
    board[to.row]![to.col] = piece
    board[from.row]![from.col] = null

    // Halfmove clock
    if (piece.type === 'pawn' || capturedPiece !== null) {
        game.halfMoveClock = 0
    } else {
        game.halfMoveClock++
    }

    // Update castling rights
    if (from.row === 7 && from.col === 0) game.castlingRights.whiteQueenSide = false
    if (from.row === 7 && from.col === 7) game.castlingRights.whiteKingSide = false
    if (from.row === 0 && from.col === 0) game.castlingRights.blackQueenSide = false
    if (from.row === 0 && from.col === 7) game.castlingRights.blackKingSide = false
    if (from.row === 7 && from.col === 4) { game.castlingRights.whiteQueenSide = false; game.castlingRights.whiteKingSide = false }
    if (from.row === 0 && from.col === 4) { game.castlingRights.blackQueenSide = false; game.castlingRights.blackKingSide = false }

    // Record move
    game.moves.push({from, to, piece})
    console.log(game.moves)

    // Change turn
    game.turn = game.turn === 'white'
        ? 'black'
        : 'white'

    // Increment fullmove number after Black moves
    if (piece.color === 'black') {
        game.fullMoveNumber++
    }

    console.log(game)

    return
}


const pieceToFen: Record<Piece['type'], string> = {
    king: 'k',
    queen: 'q',
    rook: 'r',
    bishop: 'b',
    knight: 'n',
    pawn: 'p',
}

const fenToPiece: Record<string, Piece['type']> = {
    k: 'king',
    q: 'queen',
    r: 'rook',
    b: 'bishop',
    n: 'knight',
    p: 'pawn',
}

export function loadFen(fen: string): GameState {
    const parts = fen.trim().split(/\s+/)

    if (parts.length !== 6) {
        throw new Error('Invalid FEN: expected 6 fields')
    }

    const [
        position,
        activeColor,
        castling,
        enPassant,
        halfMove,
        fullMove,
    ] = parts

    const ranks = position!.split('/')

    if (ranks.length !== 8) {
        throw new Error('Invalid FEN: expected 8 ranks')
    }

    const board: Board = [[], [], [], [], [], [], [], []]

    for (let row = 0; row < 8; row++) {
        let col = 0

        for (const char of ranks[row]!) {
            if (/[1-8]/.test(char)) {
                col += Number(char)
                continue
            }

            const type = fenToPiece[char.toLowerCase()]

            if (!type || col >= 8) {
                throw new Error(`Invalid FEN piece: ${char}`)
            }

            const piece: Piece = {
                type,
                color: char === char.toUpperCase()
                    ? 'white'
                    : 'black',
            }

            board[row]![col] = piece
            col++
        }

        if (col !== 8) {
            throw new Error(`Invalid FEN: rank ${8 - row} has ${col} squares`)
        }
    }

    if (activeColor !== 'w' && activeColor !== 'b') {
        throw new Error('Invalid FEN: active color must be w or b')
    }

    if (
        !/^(?:-|K?Q?k?q?)$/.test(castling!)
    ) {
        throw new Error('Invalid FEN: invalid castling rights')
    }

    const halfMoveClock = Number(halfMove)
    const fullMoveNumber = Number(fullMove)

    if (
        !Number.isInteger(halfMoveClock) ||
        halfMoveClock < 0 ||
        !Number.isInteger(fullMoveNumber) ||
        fullMoveNumber < 1
    ) {
        throw new Error('Invalid FEN: invalid move counters')
    }

    return {
        board,
        turn: activeColor === 'w' ? 'white' : 'black',
        castlingRights: {
            whiteKingSide: castling!.includes('K'),
            whiteQueenSide: castling!.includes('Q'),
            blackKingSide: castling!.includes('k'),
            blackQueenSide: castling!.includes('q'),
        },
        moves: [],
        halfMoveClock,
        fullMoveNumber,
    }
}

export function getFen(game: GameState): string {
    const rows: string[] = []

    // 1. Board position
    for (let row = 0; row < 8; row++) {
        let fenRow = ''
        let emptyCount = 0

        for (let col = 0; col < 8; col++) {
            const piece = game.board[row]![col]

            if (!piece) {
                emptyCount++
                continue
            }

            if (emptyCount > 0) {
                fenRow += emptyCount
                emptyCount = 0
            }

            const symbol = pieceToFen[piece.type]

            fenRow += piece.color === 'white'
                ? symbol.toUpperCase()
                : symbol
        }

        if (emptyCount > 0) {
            fenRow += emptyCount
        }

        rows.push(fenRow)
    }

    const position = rows.join('/')

    // 2. Active color
    const activeColor = game.turn === 'white' ? 'w' : 'b'

    // 3. Castling availability
    let castling = ''

    if (game.castlingRights.whiteKingSide) castling += 'K'
    if (game.castlingRights.whiteQueenSide) castling += 'Q'
    if (game.castlingRights.blackKingSide) castling += 'k'
    if (game.castlingRights.blackQueenSide) castling += 'q'

    if (castling === '') castling = '-'

    // 4. En passant target (not implemented yet)
    const enPassant = '-'

    // 5. Halfmove clock
    const halfMoveClock = game.halfMoveClock

    // 6. Fullmove number
    const fullMoveNumber = game.fullMoveNumber

    return [
        position,
        activeColor,
        castling,
        enPassant,
        halfMoveClock,
        fullMoveNumber,
    ].join(' ')
}