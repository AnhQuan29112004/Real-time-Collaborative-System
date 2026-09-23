package com.collabnote.common.query;

import com.collabnote.common.audit.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class QuerySample extends BaseEntity {
    @Id Long id;
    Long workspaceId;
    String title;
    Integer score;

    protected QuerySample() { }
    QuerySample(long id, long workspaceId, String title, int score) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.score = score;
    }
}
