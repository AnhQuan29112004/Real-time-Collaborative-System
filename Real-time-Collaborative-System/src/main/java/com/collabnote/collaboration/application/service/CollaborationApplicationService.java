package com.collabnote.collaboration.application.service;

import com.collabnote.collaboration.application.port.in.ApplyDocumentUpdateUseCase;
import com.collabnote.collaboration.application.port.in.SynchronizeDocumentUseCase;

/** Điều phối use case qua ports; chưa có logic và chưa đăng ký Spring bean. */
public class CollaborationApplicationService implements ApplyDocumentUpdateUseCase, SynchronizeDocumentUseCase {
}
