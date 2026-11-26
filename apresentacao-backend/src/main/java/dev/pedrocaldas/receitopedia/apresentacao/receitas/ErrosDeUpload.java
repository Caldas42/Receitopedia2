package dev.pedrocaldas.receitopedia.apresentacao.receitas;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
class ErrosDeUpload {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail arquivoGrande() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Envie até 5 fotos de no máximo 5 MB cada.");
    }
}
