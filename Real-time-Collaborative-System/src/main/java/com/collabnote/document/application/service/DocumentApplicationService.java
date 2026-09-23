package com.collabnote.document.application.service;

import com.collabnote.document.application.port.in.CreateDocumentUseCase;
import com.collabnote.document.application.port.in.RestoreDocumentVersionUseCase;

/** Điều phối use case qua ports; chưa có logic và chưa đăng ký Spring bean. */
public class DocumentApplicationService implements CreateDocumentUseCase, RestoreDocumentVersionUseCase {
}
