-- 1. PROJECTS TABLE
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    parent_id UUID, -- distinct from hierarchy table, useful for UI parent pointers
    name VARCHAR(255) NOT NULL,
    color VARCHAR(7), -- e.g. #FF5733
    is_archived BOOLEAN DEFAULT FALSE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ DEFAULT NULL
);

CREATE INDEX idx_projects_workspace ON projects(workspace_id);
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);

-- 2. PROJECT HIERARCHY (Closure Table for Infinite Nesting)
CREATE TABLE project_hierarchy (
    ancestor_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    descendant_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    depth INT NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id)
);

-- 3. PROJECT MEMBERS
CREATE TABLE project_members (
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER', -- 'OWNER', 'ADMIN', 'EDITOR', 'VIEWER'
    is_favorite BOOLEAN DEFAULT FALSE,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_project_members_user ON project_members(user_id);