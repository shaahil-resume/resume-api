package com.shaahil.resumeapi.ai.retrieval;

import java.util.List;

public record SearchOutcome(boolean aboveFloor, double bestScore, List<RetrievedChunk> chunks) {
}
