import type {GameState, Square} from "@/chess_logic/types.ts";


export function getLegalMoves(game: GameState, from: Square): Square[] {
    const board = game.board
    const whitesTurn = game.turn === "white"
    const { row: row, col: col } = from
    const piece = board[row]![col];
    const legalMoves: Square[] = []

    if (!piece || piece.color !== game.turn)
        return []

    const inBounds = (row: number, col: number) => row >= 0 && row < 8 && col >= 0 && col < 8

    const addIfValid = (row: number, col: number) => {
        if (!inBounds(row, col))
            return false

        const target = board[row]?.[col]

        if (!target) {
            legalMoves.push({ row, col })
            return true
        }

        if (target.color !== piece.color)
            legalMoves.push({ row, col })

        return false
    }

    const addSlidingMoves = (directions: [number, number][]) => {
        for (const [dr, dc] of directions) {
            let r = row + dr
            let c = col + dc

            while (inBounds(r, c)) {
                const target = board[r]?.[c]

                if (!target) {
                    legalMoves.push({ row: r, col: c })
                } else {
                    if (target.color !== piece.color)
                        legalMoves.push({ row: r, col: c })

                    break
                }

                r += dr
                c += dc
            }
        }
    }

    switch (piece.type) {
        case "pawn":
            const direction = whitesTurn ? -1 : 1
            const startingRow = whitesTurn ? 6 : 1

            // One square forward
            const oneRow = row + direction

            if (inBounds(oneRow, col) && !board[oneRow]?.[col]) {
                legalMoves.push({
                    row: oneRow,
                    col
                })

                // Two squares forward
                const twoRow = row + direction * 2

                if (
                    row === startingRow &&
                    inBounds(twoRow, col) &&
                    !board[twoRow]?.[col]
                ) {
                    legalMoves.push({
                        row: twoRow,
                        col
                    })
                }
            }

            // Capture diagonally
            for (const dc of [-1, 1]) {
                const captureCol = col + dc

                if (!inBounds(oneRow, captureCol))
                    continue

                const target = board[oneRow]?.[captureCol]

                if (target && target.color !== piece.color) {
                    legalMoves.push({
                        row: oneRow,
                        col: captureCol
                    })
                }
            }

            // En passant
            const lastMove = game.moves?.at(-1)

            if (
                lastMove &&
                lastMove.piece.type === "pawn" &&
                lastMove.piece.color !== piece.color &&
                lastMove.from.col === lastMove.to.col &&
                Math.abs(lastMove.to.row - lastMove.from.row) === 2 &&
                lastMove.to.row === row &&
                Math.abs(lastMove.to.col - col) === 1
            ) {
                legalMoves.push({
                    row: row + direction,
                    col: lastMove.to.col
                })
            }
            break
        case "rook":
            addSlidingMoves([
                [-1, 0],
                [1, 0],
                [0, -1],
                [0, 1],
            ])
            break
        case "bishop":
            addSlidingMoves([
                [-1, -1],
                [-1, 1],
                [1, -1],
                [1, 1],
            ])
            break
        case "knight":
            const knightOffsets: [number, number][] = [
                [-2, -1],
                [-2, 1],
                [-1, -2],
                [-1, 2],
                [1, -2],
                [1, 2],
                [2, -1],
                [2, 1],
            ]

            for (const [dr, dc] of knightOffsets) {
                addIfValid(row + dr, col + dc)
            }

            break
        case "queen":
            addSlidingMoves([
                [-1, 0],
                [1, 0],
                [0, -1],
                [0, 1],
                [-1, -1],
                [-1, 1],
                [1, -1],
                [1, 1],
            ])
            break
        case "king":
            const kingOffsets: [number, number][] = [
                [-1, -1],
                [-1, 0],
                [-1, 1],
                [0, -1],
                [0, 1],
                [1, -1],
                [1, 0],
                [1, 1],
            ]

            for (const [dr, dc] of kingOffsets) {
                addIfValid(row + dr, col + dc)
            }

            const kingRank = whitesTurn ? 7 : 0
            const queenSideCastlingAllowed = whitesTurn ? game.castlingRights.whiteQueenSide : game.castlingRights.blackQueenSide
            const kingSideCastlingAllowed = whitesTurn ? game.castlingRights.whiteKingSide : game.castlingRights.blackKingSide

            if (queenSideCastlingAllowed && board![kingRank]![3] === null && board![kingRank]![2] === null && board![kingRank]![1] === null) {
                addIfValid(kingRank, 2)
            }

            if (kingSideCastlingAllowed && board![kingRank]![5] === null && board![kingRank]![6] === null) {
                addIfValid(kingRank, 6)
            }
    }
    console.log(from, legalMoves)
    return legalMoves;
}