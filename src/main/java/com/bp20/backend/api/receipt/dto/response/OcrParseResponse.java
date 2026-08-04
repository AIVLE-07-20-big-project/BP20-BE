package com.bp20.backend.api.receipt.dto.response;

import java.util.List;

public record OcrParseResponse(
        List<String> ocrText,
        String processedImage,
        ReceiptParseResult result
) {
}
