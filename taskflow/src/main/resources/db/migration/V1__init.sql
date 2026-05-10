-- =============================================
-- V1: Initial schema for TaskFlow API
-- =============================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Roles
CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Users
CREATE TABLE users (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- User <-> Role (join table)
CREATE TABLE user_roles (
    user_id UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Projects
CREATE TABLE projects (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    owner_id    UUID        NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- User <-> Project membership
CREATE TABLE user_projects (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, project_id)
);

-- Labels
CREATE TABLE labels (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(20) NOT NULL DEFAULT '#6366f1',
    project_id UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    UNIQUE (name, project_id)
);

-- Tasks
CREATE TABLE tasks (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority    VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date    DATE,
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assignee_id UUID        REFERENCES users(id),
    reporter_id UUID        REFERENCES users(id),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- Task <-> Label (join table)
CREATE TABLE task_labels (
    task_id  UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    label_id UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);

-- Comments
CREATE TABLE comments (
    id         UUID  NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    content    TEXT  NOT NULL,
    task_id    UUID  NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id  UUID  NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Notifications (prepared for Kafka session)
CREATE TABLE notifications (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    message    TEXT        NOT NULL,
    type       VARCHAR(50) NOT NULL,
    read       BOOLEAN     NOT NULL DEFAULT FALSE,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id    UUID        REFERENCES tasks(id) ON DELETE SET NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Audit log (prepared for AOP session)
CREATE TABLE audit_log (
    id           BIGSERIAL    PRIMARY KEY,
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    VARCHAR(100),
    actor        VARCHAR(100),
    details      TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_tasks_project   ON tasks(project_id);
CREATE INDEX idx_tasks_assignee  ON tasks(assignee_id);
CREATE INDEX idx_tasks_status    ON tasks(status);
CREATE INDEX idx_tasks_due_date  ON tasks(due_date);
CREATE INDEX idx_comments_task   ON comments(task_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, read);
