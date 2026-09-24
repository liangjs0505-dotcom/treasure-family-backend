package com.treasurefamily.sale.dto;

import java.util.List;

public record RankBoardResponse(List<RankItemResponse> hot, List<RankItemResponse> cold) {
}
