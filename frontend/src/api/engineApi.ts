import type {
    AnalysisRequest,
    AnalysisResponse,
    LegalMovesRequest,
    LegalMovesResponse,
    MakeMoveRequest,
    MakeMoveResponse,
} from './dto/engineDTO'

const BASE_URL = 'http://localhost:8080/api/v1/engine'


async function post<TRequest, TResponse>(endpoint: string, request: TRequest,): Promise<TResponse> {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
        method: 'POST', headers: {
            'Content-Type': 'application/json',
        }, body: JSON.stringify(request),
    })

    if (!response.ok) {
        throw new Error(`HTTP error: ${response.status}`)
    }

    return await response.json() as TResponse
}

export function getLegalMoves(request: LegalMovesRequest,): Promise<LegalMovesResponse> {
    return post('/legalMoves/', request)
}

export function makeMove(request: MakeMoveRequest,): Promise<MakeMoveResponse> {
    return post('/move', request)
}

export function analyze(request: AnalysisRequest,): Promise<AnalysisResponse> {
    return post('/analyze', request)
}
