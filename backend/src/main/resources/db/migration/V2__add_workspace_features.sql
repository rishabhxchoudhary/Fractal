-- Add deleted_at column for Soft Deletes on Workspaces
ALTER TABLE workspaces ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;

-- Create Invitations Table
CREATE TABLE workspace_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER', -- 'ADMIN', 'MEMBER', 'VIEWER'
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    invited_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(workspace_id, email)
);

CREATE INDEX idx_workspace_invitations_token ON workspace_invitations(token);