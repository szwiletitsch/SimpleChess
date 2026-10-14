```vue
<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {createInitialGameState, getFen, loadFen} from '@/chess_logic/game'
import type {Square} from "@/chess_logic/types.ts";
import {analyze, getLegalMoves, makeMove} from "@/api/engineApi.ts";

// required for drawing chess field
const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']
const ranks = [8, 7, 6, 5, 4, 3, 2, 1]
const pieceSymbols = {
  white: {
    king: '♔',
    queen: '♕',
    rook: '♖',
    bishop: '♗',
    knight: '♘',
    pawn: '♙',
  },
  black: {
    king: '♚',
    queen: '♛',
    rook: '♜',
    bishop: '♝',
    knight: '♞',
    pawn: '♟',
  },
}

// required to highlight fields accordingly
const game = reactive(createInitialGameState())
const selectedSquare = ref<Square | null>(null)
const legalMoves = ref<Square[]>([])

async function selectSquare(rank: number, file: string) {
  const row = 8 - rank
  const col = files.indexOf(file)

  const square = { row, col }

  const piece = game.board[row]![col]
  if (piece && piece!.color === game.turn) {
    selectedSquare.value = square

    const fen = getFen(game)
    const legalSquares = (await getLegalMoves({
      fen,
      square: `${file}${rank}`,
    })).allowedSquares

    legalMoves.value = legalSquares.map(square => ({
      row: 8 - Number(square[1]),
      col: files.indexOf(square[0]!),
    }))


    return
  }

  if (!selectedSquare.value) return

  const isLegal = legalMoves.value.some(
      move => move.row === row && move.col === col,
  )

  if (isLegal) {
    const from =
        `${files[selectedSquare.value.col]}${8 - selectedSquare.value.row}`
    const to = `${file}${rank}`

    const moveResponse = await makeMove({
      fen: getFen(game),
      from,
      to,
    })

    // Apply White's move
    Object.assign(game, loadFen(moveResponse.fen))

    selectedSquare.value = null
    legalMoves.value = []

    // Let Black (the engine) respond
    const analysisResponse = await analyze({
      fen: moveResponse.fen,
      depth: 6,
    })

    // Apply Black's move
    Object.assign(game, loadFen(analysisResponse.fen))
  }



}

function handleRightClick(event: MouseEvent) {
  event.preventDefault()
  selectedSquare.value = null
}

// true = White's perspective
// false = Black's perspective
const whitesPerspective = true


const displayedFiles = computed(() =>
    whitesPerspective ? files : [...files].reverse()
)

const displayedRanks = computed(() =>
    whitesPerspective ? ranks : [...ranks].reverse()
)
</script>

<template>
  <div class="board-wrapper" @contextmenu="handleRightClick($event)">
    <div class="rank-coordinates">
      <span
          v-for="rank in displayedRanks"
          :key="rank"
      >
        {{ rank }}
      </span>
    </div>

    <div class="board-container">
      <div class="board">
        <div
            v-for="(rank, rankIndex) in displayedRanks"
            :key="rank"
            class="rank"
        >
          <div
              v-for="(file, fileIndex) in displayedFiles"
              :key="`${file}${rank}`"
              class="square"
              @click="selectSquare(rank, file)"
              :class="{
                selected: 8 - rank == selectedSquare?.row && files.indexOf(file) == selectedSquare?.col,
                legal: legalMoves.some(move => move.row === 8 - rank && move.col === files.indexOf(file)),
                light: (rank + files.indexOf(file)) % 2 === 0,
                dark: (rank + files.indexOf(file)) % 2 === 1,
              }"
          >
            <span v-if="game.board[8 - rank]![files.indexOf(file)]"
                  class="piece"
            >
              {{
                pieceSymbols[
                    game.board[8 - rank]![files.indexOf(file)]!.color
                    ][
                    game.board[8 - rank]![files.indexOf(file)]!.type
                    ]
              }}
            </span>
          </div>
        </div>
      </div>

      <!-- File coordinates -->
      <div class="file-coordinates">
        <span
            v-for="file in displayedFiles"
            :key="file"
        >
          {{ file }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-wrapper {
  display: flex;
  width: min(80vw, 720px);
}

.rank-coordinates {
  width: 24px;
  display: grid;
  grid-template-rows: repeat(8, 1fr);
  align-items: center;
  text-align: center;
  font-size: 0.7rem;
  opacity: 0.6;
}

.board-container {
  flex: 1;
}

.board {
  display: grid;
  grid-template-rows: repeat(8, 1fr);
  aspect-ratio: 1;
  border: 4px solid #222;
}

.rank {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
}

.square {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.light {
  background: #e9eace;
}

.dark {
  background: #729451;
}

.selected {
  background: #dd4242;
}

.legal::after {
  content: '';
  position: absolute;
  width: 25%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: rgba(73, 92, 51, 0.6);
}

.file-coordinates {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  height: 24px;
  align-items: center;
  text-align: center;
  font-size: 0.7rem;
  opacity: 0.6;
}

.piece {
  font-size: clamp(2rem, 7vw, 4.5rem);
  line-height: 1;
  cursor: pointer;
  user-select: none;
}
</style>
```