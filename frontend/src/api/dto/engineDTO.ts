export interface AnalysisRequest {
    fen: string;
    depth: number;
}

export interface AnalysisResponse {
    bestMove: string;
    fen: string;
    eval: number;
    depth: number;
}

export interface LegalMovesRequest {
    fen: string
    square: string
}

export interface LegalMovesResponse {
    allowedSquares: string[]
}

export interface MakeMoveRequest {
    fen: string
    from: string
    to: string
}

export interface MakeMoveResponse {
    fen: string
}