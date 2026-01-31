A comprehensive and well-structured plan for a feature-rich Todo List application.

### **Advanced Features**

- **Natural Language Task Addition:** Employ Retrieval-Augmented Generation (RAG) to convert natural language input into tasks. This will involve creating embeddings from the tasks and using a vector database like Qdrant with HNSW ANNS cosine similarity for searching.
- **Email to Task:** Provide a dedicated email address that, upon receiving a forwarded email, automatically creates a new task.
- **Browser Extension:** A browser extension to capture the current webpage as a task, ideal for creating reading lists or research items.
- **Bi-Directional API / Webhooks:** Instead of relying on pre-built integrations, offer a webhook URL for each project. This empowers power users to connect with services like IFTTT, Zapier, or custom scripts (e.g., creating a task when a GitHub issue is assigned).
- **Markdown and Rich Text Support:** The task description field will fully support Markdown, including headers, code blocks, bold, italics, and inline image rendering.
- **File Attachments:** Allow users to attach PDFs, images, or spreadsheets directly to tasks for reference.
- **Undo/Redo History:** Implement infinite undo/redo functionality for all changes within a list. Accidental project deletions can be reversed with a simple shortcut. This will be managed through a `command_log` table storing each user event.
- **Drag and Drop Functionality:** Utilize fractional indexing with an algorithm like LexoRank to enable smooth and persistent drag-and-drop ordering of tasks.
- **Local-First Application:** Design the app to work offline using Conflict-free Replicated Data Types (CRDTs) and Automerge for seamless data synchronization.
- **Attribute-Based Access Control (ABAC):** Implement fine-grained permissions (READ, CREATE, UPDATE, DELETE, COMPLETE, RESCHEDULE) using Open Policy Agent (OPA) with policies written in Rego.
- **Role-Based Access Control (RBAC):** A hierarchical permission system with roles: Owner > Admin > Editor > Viewer.

### **Schedule Management**

- **Start and End Dates:** Define start dates and deadlines for tasks.
- **Recurring Tasks:** Support for daily, weekly, and monthly recurrences based on the iCalendar (RFC 5545) standard, potentially using a library like ical4j in Java.
- **Time Blocking:** Sync tasks with Google Calendar and Apple Calendar to facilitate time blocking.
- **Timezone Intelligence:** Handle both fixed and floating timezones for tasks.

### **Structuring**

- **Infinite Nesting:** Allow for infinite nesting of projects (sub-projects) and tasks (sub-tasks) using closure tables with `Ancestor`, `Descendant`, and `Depth` columns.
- **Tags:** A flexible many-to-many relationship for tagging tasks (e.g., @Computer, #DeepWork, LowEnergy).
- **Kanban & Gantt Views:** Provide the ability to visualize project tasks as a Kanban board or a Gantt chart.

### **Workflow & Execution**

- **Task Dependencies:** Implement task dependencies where one task cannot be completed until another is finished, managed using a Directed Acyclic Graph (DAG) and topological sorting.
- **Focus Mode:** A feature to hide all UI distractions, showing only the current task.
- **Pomodoro Timer Integration:** A built-in Pomodoro timer to help users focus on tasks for 25-minute intervals.
- **Templates:** Allow users to save a list of tasks as a template (e.g., "New Client Onboarding") for reuse.
- **Task "Rot" & Auto-Archiving:** Visual cues for stale tasks, such as fading colors or icons. The app will prompt users to deal with tasks that have been repeatedly rescheduled.
- **Eisenhower Matrix View:** A dynamic four-quadrant grid that automatically sorts tasks based on their urgency and importance.
- **Soft vs. Hard Deadlines:** Differentiate between hard deadlines (must be done) and soft, goal-oriented deadlines that don't trigger "overdue" alerts if missed by a short period.
- **Notification System:** A system to send reminders and notifications to users.

### **Collaboration**

- **Shared Projects with Permissions:** Invite others to projects with specific permissions (Read Only, Can Edit, Can Admin).
- **Task Assignment:** Assign tasks within a shared project to specific individuals.
- **Comments & Activity Log:** A discussion thread within each task and a log of all changes.

### **AI Features**

- **Task Breakdown:** An AI feature to break down a large task into smaller, manageable sub-tasks.
- **Smart Rescheduling:** An AI algorithm to analyze overdue tasks and suggest a realistic new schedule based on priorities and calendar availability.
- **Context-Aware Suggestions:** The app will suggest quick tasks to complete based on the user's available time slots.

### **Security**

- **Data Encryption:** All data at rest will be encrypted.
- **Compliance:** Adherence to security standards such as SOC 2, GDPR, and ISO.

### **Analytics**

- **Productivity Pulse/Velocity:** A visual representation of task completion rates over time.
- **Estimated vs. Actual Time:** The app will track the difference between estimated and actual time spent on tasks to provide insights into estimation accuracy.
- **Circadian Rhythm Optimization:** The app will learn the user's most productive times and suggest scheduling deep work accordingly.
- **CQRS Pattern:** Utilize the Command Query Responsibility Segregation pattern for managing data.

### **Tech Stack**

- **Frontend:** Next.js (initially without local-first, to be added later).
- **Backend:** Spring Boot (Java).
- **Scalability:** The application will be designed to handle over 200 million requests.
- **AI:** AWS Bedrock for embedding models.
- **Vector Database:** Qdrant (free tier).
- **Event Logging:** Initially a `command_log` table, with plans to migrate to Kafka.
- **Database:** Writes (Commands) and initial Reads will go to a normalized PostgreSQL database. There is potential to later shift read operations to OpenSearch.
- **Development Methodology:** Test-Driven Development (TDD).
- **Initial Architecture:** Serverless approach using AWS Lambda and API Gateway, with a future migration to a load balancer and ECS Fargate to mitigate cold starts.
- **Authentication:** OAuth 2.0 with Google Sign-In as the initial provider.
- **Database Hosting:** AWS RDS for PostgreSQL.
- **Cold Start Mitigation:** AWS SnapStart will be used to reduce cold start times for Lambda functions.

* using redis for cacheing.
* use flyway for database migrations

### **Database Schema Design**

#### A. Projects & Project Hierarchy

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL, -- Logical separation for multi-tenancy
    name VARCHAR(255) NOT NULL,
    color VARCHAR(7), -- Hex code
    view_style VARCHAR(50) DEFAULT 'LIST', -- 'LIST', 'BOARD', 'CALENDAR', 'GANTT'
    is_archived BOOLEAN DEFAULT FALSE,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ DEFAULT NULL -- delete after 30 days using a cron job
);

-- Closure Table for Projects (Infinite Folder Nesting)
CREATE TABLE project_hierarchy (
    ancestor_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    descendant_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    depth INT NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id)
);

CREATE INDEX idx_project_workspace ON projects(workspace_id);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id), -- The person who pays/owns the data
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE, -- For URLs (e.g., app.todo.com/my-company)
    plan_type VARCHAR(50) DEFAULT 'FREE', -- 'FREE', 'PRO', 'ENTERPRISE'
    stripe_customer_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE workspace_members (
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER', -- 'OWNER', 'ADMIN', 'MEMBER'
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (workspace_id, user_id)
);

CREATE TABLE project_members (
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER', -- 'OWNER', 'ADMIN', 'EDITOR', 'VIEWER'
    is_favorite BOOLEAN DEFAULT FALSE,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE workspace_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    invited_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(workspace_id, email)
);
```

#### B. Tasks & Attributes

```sql
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    parent_task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT, -- Markdown content
    priority SMALLINT DEFAULT 0, -- 1=High, 2=Med, 3=Low, 0=None
    status VARCHAR(50) DEFAULT 'TODO', -- 'TODO', 'IN_PROGRESS', 'DONE', 'CANCELED'
    bucket_id UUID, -- Specific column ID if using Kanban board view
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    archived BOOLEAN DEFAULT FALSE,
    lexorank VARCHAR(255) NOT NULL,
    start_date TIMESTAMPTZ,
    due_date TIMESTAMPTZ,
    soft_deadline TIMESTAMPTZ,
    is_hard_deadline BOOLEAN DEFAULT FALSE,
    recurrence_rule TEXT, -- RFC 5545
    timezone VARCHAR(50) DEFAULT 'UTC',
    embedding_id UUID,
    estimated_minutes INT,
    time_spent_minutes INT DEFAULT 0,
    version BIGINT DEFAULT 1,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    search_vector tsvector
);

-- Closure Table for Tasks (Infinite Sub-task Nesting)
CREATE TABLE task_hierarchy (
    ancestor_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    descendant_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    depth INT NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id)
);

CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_parent ON tasks(parent_task_id);
CREATE INDEX idx_tasks_lexorank ON tasks(project_id, lexorank);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);

CREATE TABLE task_recurrence_exceptions (
    id UUID PRIMARY KEY,
    original_task_id UUID REFERENCES tasks(id),
    original_date TIMESTAMPTZ,
    new_date TIMESTAMPTZ,
    is_completed BOOLEAN,
    is_deleted BOOLEAN,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### C. Dependencies (DAG)

```sql
CREATE TABLE task_dependencies (
    blocker_task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    blocked_task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    dependency_type VARCHAR(20) DEFAULT 'FINISH_TO_START',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (blocker_task_id, blocked_task_id)
);
```

#### D. Tags (Many-to-Many)

```sql
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(workspace_id, name)
);

CREATE TABLE task_tags (
    task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);
```

#### E. Email to Task

```sql
CREATE TABLE inbound_email_routing (
    email_slug VARCHAR(255) PRIMARY KEY,
    target_project_id UUID,
    user_id UUID
);
```

#### F. Command Log

```sql
CREATE TABLE command_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID,
    user_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(20), -- 'TASK', 'PROJECT', 'TAG'
    action_type VARCHAR(50), -- 'CREATE', 'UPDATE_FIELD', 'DELETE'
    previous_state JSONB,
    new_state JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_command_log_transaction ON command_log(transaction_id);
```

#### G. Attachments

```sql
CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID REFERENCES tasks(id) ON DELETE CASCADE,
    uploader_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size_bytes BIGINT,
    s3_bucket VARCHAR(64) NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    storage_provider VARCHAR(20) DEFAULT 'AWS_S3',
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_attachments_task ON attachments(task_id);
```

#### H. Comments & Activity

```sql
CREATE TABLE task_comments (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES tasks(id),
    user_id UUID,
    content TEXT, -- Supports Markdown
    parent_comment_id UUID REFERENCES task_comments(id),
    thread_path ltree,
    created_at TIMESTAMPTZ
);
```

#### I. User Settings

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    timezone VARCHAR(50) DEFAULT 'UTC',
    daily_digest_time TIME,
    theme_preference VARCHAR(20),
    created_at TIMESTAMPTZ
);

CREATE TABLE user_identities (
    user_id UUID REFERENCES users(id),
    provider VARCHAR(20), -- 'GOOGLE', 'GITHUB'
    provider_id VARCHAR(255),
    PRIMARY KEY (user_id, provider)
);

CREATE INDEX idx_users_email ON users(email);
```

#### J. Webhooks

```sql
CREATE TABLE webhook_subscriptions (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    target_url TEXT,
    events TEXT[], -- ['TASK_CREATED', 'TASK_COMPLETED']
    secret_key VARCHAR(255),
    created_at TIMESTAMPTZ
);
```

Steps:

1. https://github.com/vercel/platforms cloned this repo in the /frontend folder. I am building a multi tenant application where each user can create his workspace, every work space will have its own subdomain... I also setup redis, i have KV_REST_API_URL, KV_REST_API_TOKEN from upstash.

- Created a spring initializer project with dependencies:
  Spring Web (For building REST APIs)
  Spring Data JPA (For database interaction)
  PostgreSQL Driver (Connects to Postgres)
  Flyway Migration (Database version control)
  Lombok (Reduces code boilerplate)
  Spring Security (Authentication foundation)
  OAuth2 Client (For Google Login)
  Testcontainers (For running real DBs in tests - crucial for TDD)
- put it in /backend folder
- created docker-compose.yml with this content:

```
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    container_name: todo_postgres
    environment:
      POSTGRES_DB: todo_db
      POSTGRES_USER: todo_user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
```

- open a separate terminal in backend folder and `docker-compose up -d`

in the src/main/resources/application.properties, added this config:

```
spring.application.name=fractal

# Database Configuration
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/todo_db}
spring.datasource.username=${DB_USERNAME:todo_user}
spring.datasource.password=${DB_PASSWORD:password}

# Flyway (Database Migration)
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# OAuth2 Google Configuration (We will fill these later)
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

app.frontend.url=http://localhost:3000
```

Ran the test using `mvn test` command and it failed.

- in the backend/src/main/java/com/fractal/backend/controller/HealthCheckController.java
  add this:

```
package com.fractal.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.model.User;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @GetMapping("/health")
    public String healthCheck() {
        return "System Operational";
    }

    @GetMapping("/protected")
    public String protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        return "This is a protected resource.";
    }
}
```

- in the backend/src/test/java/com/fractal/controller/HealthCheckControllerTest.java

```
package com.fractal.backend.controller;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.fractal.backend.security.JwtAuthenticationFilter;
import com.fractal.backend.service.JwtService;

@WebMvcTest(HealthCheckController.class)
@AutoConfigureMockMvc(addFilters = false)
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithAuth() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isOk());
    }
}
```

- in the backend/src/main/resources/db/migration/V1\_\_init_schema.sql i have added:

```
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS ltree;

-- 1. USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. WORKSPACES TABLE
CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE,
    plan_type VARCHAR(50) DEFAULT 'FREE',
    stripe_customer_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. WORKSPACE MEMBERS
CREATE TABLE workspace_members (
    workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (workspace_id, user_id)
);

-- 4. USER IDENTITIES (For OAuth)
CREATE TABLE user_identities (
    user_id UUID REFERENCES users(id),
    provider VARCHAR(20), -- 'GOOGLE', 'GITHUB'
    provider_id VARCHAR(255),
    PRIMARY KEY (user_id, provider)
);
```

- in backend/src/main/resources/db/migration/V2\_\_add_workspace_features.sql

```sql
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
```

- in the `backend/src/test/java/com/fractal/repository/UserRepositoryTest.java`

```
package com.fractal.repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    // This spins up a temporary Postgres DB in Docker just for this test
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16"); // Or another version

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        // Arrange
        User newUser = new User();
        newUser.setEmail("test@fractal.com");
        newUser.setFullName("Test User");

        // Act
        userRepository.save(newUser);
        Optional<User> foundUser = userRepository.findByEmail("test@fractal.com");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@fractal.com");
    }
}
```

- in the `backend/src/main/java/com/fractal/backend/model/User.java`

```
package com.fractal.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "is_active")
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

- in the backend/src/main/java/com/fractal/backend/repository/UserRepository.java

```
package com.fractal.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Spring Data JPA automatically writes the SQL for this based on the method name
    Optional<User> findByEmail(String email);
}
```

In pom.xml, i added:

```
        <dependency>
            <groupId>io.github.cdimascio</groupId>
            <artifactId>java-dotenv</artifactId>
            <version>5.2.2</version>
        </dependency>

        <!-- Validation (e.g., @NotNull, @Email) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

updated the backend/src/main/java/com/fractal/FractalApplication.java to use .env

```
package com.fractal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class FractalApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // Don't crash if file is missing (e.g., in Prod/Docker)
                .load();
        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(FractalApplication.class, args);
    }
}
```

- in the src/test/java/com/fractal/service/AuthServiceTest.java

```
package com.fractal.service;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito for this test class
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.fractal.backend.repository.WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_ShouldCreateNewUser_WhenUserDoesNotExist() {
        // Arrange
        String email = "new@example.com";
        String name = "New User";
        String avatar = "http://avatar.url";

        // Mock DB returning empty (User not found)
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Mock DB saving a user (return the user that was passed in)
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(java.util.UUID.randomUUID()); // Simulate DB generating ID
            return u;
        });

        // Act
        LoginResponse response = authService.loginOrSignup(email, name, avatar);

        // Assert
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(email);
        assertThat(response.getUser().getFullName()).isEqualTo(name);
        assertThat(response.getWorkspaces()).isEmpty(); // New user has no workspaces

        // Verify save was called exactly once
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_ShouldReturnExistingUser_WhenUserExists() {
        // Arrange
        String email = "existing@example.com";
        User existingUser = new User();
        existingUser.setEmail(email);
        existingUser.setFullName("Old Name");

        // Mock DB finding the user
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // Act
        LoginResponse response = authService.loginOrSignup(email, "New Name", "avatar");

        // Assert
        assertThat(response.getUser()).isEqualTo(existingUser);
        verify(userRepository, never()).save(any(User.class)); // Should NOT save again
    }
}
```

in backend/src/main/java/com/fractal/backend/service/AuthService.java

```
package com.fractal.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository; // Inject this
    private final WorkspaceRepository workspaceRepository; // Inject this

    @Transactional
    public LoginResponse loginOrSignup(String email, String fullName, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setAvatarUrl(avatarUrl);
                    return userRepository.save(newUser);
                });

        List<WorkspaceMember> memberships = workspaceMemberRepository.findAllByUserId(user.getId());

        // 2. Map to DTOs
        List<LoginResponse.WorkspaceDTO> workspaceDTOs = memberships.stream().map(member -> {
            Workspace w = workspaceRepository.findById(member.getWorkspaceId()).orElse(null);
            if (w == null)
                return null;

            return LoginResponse.WorkspaceDTO.builder()
                    .id(w.getId())
                    .name(w.getName())
                    .slug(w.getSlug())
                    .role(member.getRole())
                    .build();
        }).filter(dto -> dto != null).collect(Collectors.toList());

        return LoginResponse.builder()
                .user(user)
                .workspaces(workspaceDTOs)
                .build();
    }
}
```

in backend/src/main/java/com/fractal/backend/dto/LoginResponse.java

```
package com.fractal.backend.dto;

import java.util.List;
import java.util.UUID;

import com.fractal.backend.model.User;

import lombok.Builder;
import lombok.Data;

// @Data generates getters, setters, toString, etc.
// @Builder creates the builder pattern (e.g., LoginResponse.builder().user(u).build())
@Data
@Builder
public class LoginResponse {
    private User user;
    private List<WorkspaceDTO> workspaces;

    // We can define a "nested" DTO right inside here for convenience
    @Data
    @Builder
    public static class WorkspaceDTO {
        private UUID id;
        private String name;
        private String slug; // Good to send this to the frontend
        private String role; // The role of the current user in this workspace
    }
}
```

- backend/src/main/java/com/fractal/backend/config/SecurityConfig.java

```
package com.fractal.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.fractal.backend.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        @Order(1)
        public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/oauth2/**", "/login/**")
                                .cors(cors -> cors.disable())
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2.successHandler(customOAuth2AuthenticationSuccessHandler));
                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())

                                // 🚫 NO REDIRECTS — API behavior
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // --- DEBUG LOG START ---
                                                        System.err.println(
                                                                        ">>> [SECURITY ERROR] 401 Unauthorized Triggered in EntryPoint");
                                                        System.err.println(">>> [SECURITY ERROR] Exception: "
                                                                        + authException.getMessage());
                                                        System.err.println(">>> [SECURITY ERROR] Request URI: "
                                                                        + request.getRequestURI());
                                                        // --- DEBUG LOG END ---

                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                }))

                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/health").permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of(
                                "http://localhost:3000",
                                "http://lvh.me:3000",
                                "http://*.lvh.me:3000",
                                "https://app.rishabhxchoudhary.com",
                                "https://*.app.rishabhxchoudhary.com"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", configuration);
                return source;
        }
}
```

- in backend/src/main/java/com/fractal/backend/config/WebConfig.java

```
package com.fractal.backend.security;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import com.fractal.backend.service.AuthService;
import com.fractal.backend.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");

        // 1. Create/Find User in DB (We still need to ensure they exist)
        authService.loginOrSignup(email, name, avatarUrl);

        // 2. Generate JWT Token
        String token = jwtService.generateToken(email);

        // 3. Redirect ONLY to the callback page with the token
        // The Frontend will decide where to go next based on user data
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("token", token)
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }
}
```

- in backend/src/test/java/com/fractal/backend/security/CustomOAuth2AuthenticationSuccessHandlerTest.java

```
package com.fractal.backend.security;

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.service.AuthService;
import com.fractal.backend.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    private OAuth2User oAuth2User;
    private final String frontendUrl = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        // Manually set the frontendUrl field since @Value won't work in a plain Mockito
        // test
        ReflectionTestUtils.setField(successHandler, "frontendUrl", frontendUrl);

        Map<String, Object> attributes = Map.of(
                "email", "test.user@google.com",
                "name", "Test User",
                "picture", "http://example.com/avatar.jpg");
        oAuth2User = new DefaultOAuth2User(Collections.emptyList(), attributes, "email");
    }

    @Test
    void onAuthenticationSuccess_newUser_redirectsToNewWorkspace() throws IOException, ServletException {
        // --- Arrange ---
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(), "google");

        User user = new User();
        user.setEmail("test.user@google.com");
        LoginResponse loginResponse = LoginResponse.builder()
                .user(user)
                .workspaces(new ArrayList<>()) // No workspaces
                .build();

        when(authService.loginOrSignup(anyString(), anyString(), anyString())).thenReturn(loginResponse);
        when(jwtService.generateToken(anyString())).thenReturn("mock-jwt-token");

        // --- Act ---
        successHandler.onAuthenticationSuccess(request, response, token);

        // --- Assert ---
        verify(authService).loginOrSignup(
                "test.user@google.com",
                "Test User",
                "http://example.com/avatar.jpg");

        // Verify that the redirect goes to the "new workspace" page
        verify(response).sendRedirect(contains("token=mock-jwt-token"));
        verify(response).sendRedirect(frontendUrl + "/auth/callback?token=mock-jwt-token");
    }

    @Test
    void onAuthenticationSuccess_existingUser_redirectsToDashboard() throws IOException, ServletException {
        // --- Arrange ---
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(), "google");

        User user = new User();
        user.setEmail("test.user@google.com");

        // Simulate a user that has an existing workspace
        List<LoginResponse.WorkspaceDTO> workspaces = List.of(
                LoginResponse.WorkspaceDTO.builder()
                        .id(UUID.randomUUID())
                        .name("My Workspace")
                        .slug("my-workspace")
                        .role("OWNER")
                        .build());
        LoginResponse loginResponse = LoginResponse.builder()
                .user(user)
                .workspaces(workspaces)
                .build();

        when(authService.loginOrSignup(anyString(), anyString(), anyString())).thenReturn(loginResponse);
        when(jwtService.generateToken(anyString())).thenReturn("mock-jwt-token");

        // --- Act ---
        successHandler.onAuthenticationSuccess(request, response, token);

        // --- Assert ---
        verify(authService).loginOrSignup(
                "test.user@google.com",
                "Test User",
                "http://example.com/avatar.jpg");

        verify(response).sendRedirect(frontendUrl + "/auth/callback?token=mock-jwt-token");
    }
}
```

created a project in google developer console called 'fractal',configured oauth concent screen, went to APIs & Services > Credentials and created oauth2 client added origin: http://localhost:8080 and redirect URI: http://localhost:8080/login/oauth2/code/google, added the credentials to .env in backend and frontend folder.

- in backend/src/main/java/com/fractal/backend/controller/WorkspaceController.java

```
package com.fractal.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.InviteMemberRequest;
import com.fractal.backend.dto.TransferOwnershipRequest;
import com.fractal.backend.dto.UpdateMemberRoleRequest;
import com.fractal.backend.dto.UpdateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // --- HELPER FOR AUTH CHECK ---
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    // --- WORKSPACE CRUD ---

    @PostMapping
    public WorkspaceResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        User user = getAuthenticatedUser(); // Using helper to reduce code duplication, but same logic
        Workspace workspace = workspaceService.createWorkspace(user.getId(), request.getName());

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .role("OWNER")
                .build();
    }

    @GetMapping
    public List<WorkspaceResponse> getUserWorkspaces() {
        User user = getAuthenticatedUser();
        return workspaceService.getWorkspacesForUser(user.getId());
    }

    @PutMapping("/{id}")
    public WorkspaceResponse updateWorkspace(
            @PathVariable UUID id,
            @RequestBody UpdateWorkspaceRequest request) {
        User user = getAuthenticatedUser();
        Workspace updated = workspaceService.updateWorkspace(user.getId(), id, request.getName(), request.getSlug());
        return WorkspaceResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .slug(updated.getSlug())
                .role("UNKNOWN")
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        workspaceService.deleteWorkspace(user.getId(), id);
    }

    // --- MEMBER MANAGEMENT ---

    @GetMapping("/{id}/members")
    public List<WorkspaceMemberDTO> getWorkspaceMembers(@PathVariable UUID id) {
        User user = getAuthenticatedUser();
        return workspaceService.getWorkspaceMembers(user.getId(), id);
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID id,
            @PathVariable UUID targetUserId) {
        User user = getAuthenticatedUser();
        workspaceService.removeMember(user.getId(), id, targetUserId);
    }

    @PutMapping("/{id}/members/{targetUserId}")
    public void updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        User user = getAuthenticatedUser();
        workspaceService.updateMemberRole(user.getId(), id, targetUserId, request.getRole());
    }

    @PostMapping("/{id}/transfer-ownership")
    public void transferOwnership(
            @PathVariable UUID id,
            @Valid @RequestBody TransferOwnershipRequest request) {
        User user = getAuthenticatedUser();
        workspaceService.transferOwnership(user.getId(), id, request.getNewOwnerId());
    }

    // --- INVITATIONS ---

    @PostMapping("/{id}/invite")
    public void inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request) {
        User user = getAuthenticatedUser();
        workspaceService.inviteMember(user.getId(), id, request.getEmail(), request.getRole());
    }

    @PostMapping("/accept-invite")
    public void acceptInvite(@RequestParam String token) {
        User user = getAuthenticatedUser();
        workspaceService.acceptInvitation(user.getId(), token);
    }
}
```

- in backend/src/main/java/com/fractal/backend/dto/TransferOwnershipRequest.java

```
package com.fractal.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferOwnershipRequest {
    @NotNull
    private UUID newOwnerId;
}
```

- in backend/src/main/java/com/fractal/backend/dto/UpdateMemberRoleRequest.java

```
package com.fractal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMemberRoleRequest {
    @NotBlank
    private String role; // ADMIN, MEMBER, VIEWER
}
```

- in backend/src/main/java/com/fractal/backend/dto/WorkspaceMemberDTO.java

```
package com.fractal.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberDTO {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private OffsetDateTime joinedAt;
}
```

- in backend/src/main/java/com/fractal/backend/dto/CreateWorkspaceRequest.java

```
package com.fractal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateWorkspaceRequest {
    @NotBlank(message = "Workspace name is required")
    private String name;
}
```

- in backend/src/main/java/com/fractal/backend/dto/WorkspaceResponse.java

```
package com.fractal.backend.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkspaceResponse {
    private UUID id;
    private String name;
    private String slug;
    private String role; // "OWNER"
}
```

- in backend/src/main/java/com/fractal/backend/model/Workspace.java

```
package com.fractal.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // The person who pays/owns the data
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(name = "plan_type")
    @Builder.Default
    private String planType = "FREE";

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
```

- in backend/src/main/java/com/fractal/backend/model/WorkspaceInvitation.java

```
package com.fractal.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String email;

    @Builder.Default
    private String role = "MEMBER";

    @Column(nullable = false)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
```

- in backend/src/main/java/com/fractal/backend/repository/WorkspaceInvitationRepository.java

```
package com.fractal.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.WorkspaceInvitation;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {
    Optional<WorkspaceInvitation> findByToken(String token);
    void deleteByWorkspaceIdAndEmail(UUID workspaceId, String email);
}
```

- in backend/src/main/java/com/fractal/backend/model/WorkspaceMember.java

```
package com.fractal.backend.model;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(WorkspaceMember.WorkspaceMemberId.class) // Defines Composite Key
public class WorkspaceMember {

    @Id
    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "role")
    @Builder.Default
    private String role = "MEMBER";

    @CreationTimestamp
    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    // Inner class for Composite Key
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceMemberId implements Serializable {
        private UUID workspaceId;
        private UUID userId;
    }
}
```

- in backend/src/main/java/com/fractal/backend/repository/WorkspaceMemberRepository.java

```
package com.fractal.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.model.WorkspaceMember;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMember.WorkspaceMemberId> {
    List<WorkspaceMember> findAllByUserId(UUID userId);

    // Fetch members with User details using a DTO projection
    @Query("SELECT new com.fractal.backend.dto.WorkspaceMemberDTO(u.id, u.email, u.fullName, u.avatarUrl, wm.role, wm.joinedAt) "
            +
            "FROM WorkspaceMember wm " +
            "JOIN User u ON wm.userId = u.id " +
            "WHERE wm.workspaceId = :workspaceId " +
            "ORDER BY wm.joinedAt ASC")
    List<WorkspaceMemberDTO> findMembersByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
```

- in backend/src/main/java/com/fractal/backend/repository/WorkspaceRepository.java

```
package com.fractal.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.Workspace;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsBySlug(String slug);

    List<Workspace> findAllByOwnerId(UUID ownerId);
}
```

- in backend/src/main/java/com/fractal/backend/service/WorkspaceService.java

```
package com.fractal.backend.service;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.WorkspaceMemberDTO;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceInvitation;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceInvitationRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService; // Inject Email Service

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public Workspace createWorkspace(UUID userId, String name) {
        String slug = toSlug(name);
        String originalSlug = slug;
        int count = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + count;
            count++;
        }

        Workspace workspace = Workspace.builder()
                .ownerId(userId)
                .name(name)
                .slug(slug)
                .planType("FREE")
                .build();
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(savedWorkspace.getId())
                .userId(userId)
                .role("OWNER")
                .build();
        workspaceMemberRepository.save(member);
        return savedWorkspace;
    }

    public List<WorkspaceResponse> getWorkspacesForUser(UUID userId) {
        List<Workspace> workspaces = workspaceRepository.findAllActiveWorkspacesByUserId(userId);

        return workspaces.stream().map(w -> {
            WorkspaceMember member = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(w.getId(), userId).orElse(null);
            return WorkspaceResponse.builder()
                    .id(w.getId())
                    .name(w.getName())
                    .slug(w.getSlug())
                    .role(member != null ? member.getRole() : "UNKNOWN")
                    .build();
        }).collect(Collectors.toList());
    }

    public List<WorkspaceMemberDTO> getWorkspaceMembers(UUID requesterId, UUID workspaceId) {
        // All members (OWNER, ADMIN, MEMBER) can view workspace members
        validateRole(workspaceId, requesterId, List.of("OWNER", "ADMIN", "MEMBER"));
        return workspaceMemberRepository.findMembersByWorkspaceId(workspaceId);
    }

    @Transactional
    public Workspace updateWorkspace(UUID userId, UUID workspaceId, String newName, String newSlug) {
        validateRole(workspaceId, userId, List.of("OWNER", "ADMIN"));
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        if (newName != null && !newName.isBlank()) {
            workspace.setName(newName);
        }
        if (newSlug != null && !newSlug.isBlank() && !newSlug.equals(workspace.getSlug())) {
            if (workspaceRepository.existsBySlug(newSlug)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
            }
            workspace.setSlug(toSlug(newSlug));
        }
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void updateMemberRole(UUID requesterId, UUID workspaceId, UUID targetUserId, String newRole) {
        // Only OWNER can update member roles
        validateRole(workspaceId, requesterId, List.of("OWNER"));

        // Validate new role is valid (must be ADMIN or MEMBER, not OWNER)
        validateRoleValue(newRole);

        if ("OWNER".equals(newRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ownership transfer must be done via specific endpoint");
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if ("OWNER".equals(targetMember.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change role of the Workspace Owner");
        }

        targetMember.setRole(newRole.toUpperCase());
        workspaceMemberRepository.save(targetMember);
    }

    // --- MEMBER MANAGEMENT (REMOVE MEMBER) ---
    @Transactional
    public void removeMember(UUID requesterId, UUID workspaceId, UUID targetUserId) {
        WorkspaceMember requester = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        WorkspaceMember target = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        // Logic:
        // 1. User can leave (requester == target), unless they are OWNER
        // 2. Only OWNER can remove other members

        if (requesterId.equals(targetUserId)) {
            // Leaving - any member can leave except OWNER
            if ("OWNER".equals(target.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Owner cannot leave workspace. Delete workspace or transfer ownership.");
            }
        } else {
            // Removing another member - only OWNER can do this
            if (!"OWNER".equals(requester.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only workspace owners can remove members");
            }
        }

        workspaceMemberRepository.delete(target);
    }

    // --- DELETE WORKSPACE ---
    @Transactional
    public void deleteWorkspace(UUID userId, UUID workspaceId) {
        validateRole(workspaceId, userId, List.of("OWNER"));
        Workspace workspace = getWorkspaceOrThrow(workspaceId);
        workspace.setDeletedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);
    }

    // --- INVITATIONS ---
    @Transactional
    public void inviteMember(UUID requesterId, UUID workspaceId, String email, String role) {
        // OWNER and ADMIN can invite members
        validateRole(workspaceId, requesterId, List.of("OWNER", "ADMIN"));

        // Validate role value
        validateRoleValue(role);

        // Only OWNER can invite ADMIN
        WorkspaceMember requester = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));

        if ("ADMIN".equals(role) && !"OWNER".equals(requester.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only workspace owners can invite admins");
        }

        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            boolean isMember = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(workspaceId, existingUser.get().getId()).isPresent();
            if (isMember) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member");
            }
        }

        workspaceInvitationRepository.deleteByWorkspaceIdAndEmail(workspaceId, email);

        String token = UUID.randomUUID().toString();
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .email(email)
                .role(role)
                .token(token)
                .invitedBy(requesterId)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        workspaceInvitationRepository.save(invitation);
        emailService.sendWorkspaceInvite(email, workspace.getName(), token);
    }

    @Transactional
    public WorkspaceMember acceptInvitation(UUID userId, String token) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid or expired invitation"));

        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired");
        }

        // Check if already member
        if (workspaceMemberRepository.findByWorkspaceIdAndUserId(invitation.getWorkspaceId(), userId).isPresent()) {
            workspaceInvitationRepository.delete(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this workspace");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(invitation.getWorkspaceId())
                .userId(userId)
                .role(invitation.getRole())
                .build();

        WorkspaceMember savedMember = workspaceMemberRepository.save(member);
        workspaceInvitationRepository.delete(invitation);
        return savedMember;
    }

    // --- HELPERS ---
    public Workspace getWorkspaceOrThrow(UUID workspaceId) {
        Workspace w = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        if (w.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        return w;
    }

    private void validateRole(UUID workspaceId, UUID userId, List<String> allowedRoles) {
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: Not a member of this workspace"));

        if (!allowedRoles.contains(member.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    /**
     * Validates that the role value is one of the allowed roles: OWNER, ADMIN, or MEMBER
     * @param role The role to validate
     * @throws ResponseStatusException if role is invalid
     */
    private void validateRoleValue(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role cannot be null or empty");
        }

        List<String> validRoles = List.of("OWNER", "ADMIN", "MEMBER");
        if (!validRoles.contains(role.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role. Allowed roles are: OWNER, ADMIN, MEMBER");
        }
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return NONLATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);
    }

    @Transactional
    public void transferOwnership(UUID currentOwnerId, UUID workspaceId, UUID newOwnerId) {
        // 1. Validate the Workspace
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        // 2. Validate Current Owner (Must be the actual owner)
        if (!workspace.getOwnerId().equals(currentOwnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the current owner can transfer ownership");
        }

        // 3. Validate New Owner (Must be a member of the workspace)
        WorkspaceMember newOwnerMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, newOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "New owner must be a member of the workspace"));

        // 4. Get Current Owner Member Record
        WorkspaceMember currentOwnerMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentOwnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Current owner member record not found"));

        // 5. Swap Roles
        // Demote current owner to ADMIN (safest default)
        currentOwnerMember.setRole("ADMIN");
        // Promote new owner to OWNER
        newOwnerMember.setRole("OWNER");

        // 6. Update Workspace Table
        workspace.setOwnerId(newOwnerId);

        // 7. Save Changes
        workspaceMemberRepository.save(currentOwnerMember);
        workspaceMemberRepository.save(newOwnerMember);
        workspaceRepository.save(workspace);
    }
}
```

- in backend/src/test/java/com/fractal/backend/controller/WorkspaceControllerTest.java

```
package com.fractal.backend.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.InviteMemberRequest;
import com.fractal.backend.dto.UpdateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.security.JwtAuthenticationFilter;
import com.fractal.backend.service.WorkspaceService;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private WorkspaceService workspaceService;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Test
        void createWorkspace_ShouldReturnCreatedWorkspace() throws Exception {
                UUID userId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                Workspace workspace = Workspace.builder()
                                .id(UUID.randomUUID())
                                .name("My New Company")
                                .slug("my-new-company")
                                .ownerId(userId)
                                .build();

                when(workspaceService.createWorkspace(eq(userId), eq("My New Company")))
                                .thenReturn(workspace);

                CreateWorkspaceRequest request = new CreateWorkspaceRequest();
                request.setName("My New Company");

                mockMvc.perform(post("/api/workspaces")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("My New Company"));
        }

        @Test
        void getUserWorkspaces_ShouldReturnList() throws Exception {
                UUID userId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                WorkspaceResponse ws = WorkspaceResponse.builder()
                                .id(UUID.randomUUID())
                                .name("WS 1")
                                .slug("ws-1")
                                .role("OWNER")
                                .build();

                when(workspaceService.getWorkspacesForUser(userId))
                                .thenReturn(List.of(ws));

                mockMvc.perform(get("/api/workspaces"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("WS 1"));
        }

        @Test
        void updateWorkspace_ShouldReturnUpdatedWorkspace() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                Workspace updatedWorkspace = Workspace.builder()
                                .id(workspaceId)
                                .name("Updated Company")
                                .slug("updated-company")
                                .ownerId(userId)
                                .build();

                when(workspaceService.updateWorkspace(eq(userId), eq(workspaceId), eq("Updated Company"),
                                eq("updated-company")))
                                .thenReturn(updatedWorkspace);

                UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
                request.setName("Updated Company");
                request.setSlug("updated-company");

                mockMvc.perform(put("/api/workspaces/" + workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Company"))
                                .andExpect(jsonPath("$.slug").value("updated-company"))
                                .andExpect(jsonPath("$.role").value("UNKNOWN"));
        }

        @Test
        void deleteWorkspace_ShouldReturnNoContent() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                doNothing().when(workspaceService).deleteWorkspace(eq(userId), eq(workspaceId));

                mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void inviteMember_ShouldReturnOk() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                doNothing().when(workspaceService).inviteMember(eq(userId), eq(workspaceId), eq("new@example.com"),
                                eq("MEMBER"));

                InviteMemberRequest request = new InviteMemberRequest();
                request.setEmail("new@example.com");
                request.setRole("MEMBER");

                mockMvc.perform(post("/api/workspaces/" + workspaceId + "/invite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void acceptInvite_ShouldReturnOk() throws Exception {
                UUID userId = UUID.randomUUID();

                User user = new User();
                user.setId(userId);

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

                WorkspaceMember member = WorkspaceMember.builder().build();

                when(workspaceService.acceptInvitation(eq(userId), eq("test-token")))
                                .thenReturn(member);

                mockMvc.perform(post("/api/workspaces/accept-invite")
                                .with(csrf())
                                .param("token", "test-token"))
                                .andExpect(status().isOk());
        }
}
```

- in backend/src/main/java/com/fractal/backend/service/EmailService.java

```
package com.fractal.backend.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendWorkspaceInvite(String toEmail, String workspaceName, String inviteToken) {
        String inviteLink = frontendUrl + "/auth/invite?token=" + inviteToken;

        Email from = new Email("rishabh26072003@gmail.com"); // Use a verified sender ID in SendGrid
        String subject = "You've been invited to join " + workspaceName;
        Email to = new Email(toEmail);

        String htmlContent = String.format(
                "<h1>Join %s on Fractal</h1>" +
                        "<p>You have been invited to collaborate on <strong>%s</strong>.</p>" +
                        "<p><a href='%s' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Accept Invitation</a></p>"
                        +
                        "<p>Or copy this link: %s</p>",
                workspaceName, workspaceName, inviteLink, inviteLink);

        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                log.error("Failed to send email: " + response.getBody());
            } else {
                log.info("Invitation email sent to " + toEmail);
            }
        } catch (IOException ex) {
            log.error("Error sending email", ex);
        }
    }
}
```

- in backend/src/test/java/com/fractal/service/WorkspaceServiceTest.java

```
package com.fractal.service;

import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;
import com.fractal.backend.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_ShouldSaveWorkspaceAndAddOwner() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@fractal.com");

        String workspaceName = "Fractal Inc";

        // Mock checking for slug uniqueness (return false means slug is not taken)
        when(workspaceRepository.existsBySlug(anyString())).thenReturn(false);

        // Mock saving workspace (return what passed in)
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> {
            Workspace w = i.getArgument(0);
            w.setId(UUID.randomUUID()); // Simulate DB ID generation
            return w;
        });

        // Act
        Workspace createdWorkspace = workspaceService.createWorkspace(user.getId(), workspaceName);

        // Assert
        assertThat(createdWorkspace).isNotNull();
        assertThat(createdWorkspace.getName()).isEqualTo(workspaceName);
        assertThat(createdWorkspace.getOwnerId()).isEqualTo(user.getId());
        assertThat(createdWorkspace.getSlug()).startsWith("fractal-inc");

        // Verify that the user was added as a member with OWNER role
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());

        WorkspaceMember member = memberCaptor.getValue();
        assertThat(member.getUserId()).isEqualTo(user.getId());
        assertThat(member.getRole()).isEqualTo("OWNER");
    }
}
```

- in backend/src/main/java/com/fractal/backend/controller/AuthController.java

```
package com.fractal.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fractal.backend.model.User;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal User user) {
        // The 'user' is injected by our JwtAuthenticationFilter
        return user;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        // Since we are stateless (JWT), logout is mostly client-side (clearing
        // localStorage).
        // But we can clear any cookies if we set them.
    }
}
```

- in backend/src/main/java/com/fractal/backend/security/JwtAuthenticationFilter.java

```
package com.fractal.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @lombok.NonNull HttpServletRequest request,
            @lombok.NonNull HttpServletResponse response,
            @lombok.NonNull FilterChain filterChain) throws ServletException, IOException {

        // --- DEBUG LOG 1: Entry Point ---
        System.out.println(">>> [JWT FILTER] Entered filter for URI: " + request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");

        // --- DEBUG LOG 2: Check Header ---
        System.out.println(">>> [JWT FILTER] Authorization Header found: " + (authHeader != null));

        final String jwt;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // --- DEBUG LOG 3: No Token Logic ---
            System.out.println(">>> [JWT FILTER] No Bearer token found. Passing request down the chain.");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "
        try {
            userEmail = jwtService.extractUsername(jwt);

            // 2. If user is found and not already authenticated in this context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3. Load user from DB
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && jwtService.isTokenValid(jwt, user.getEmail())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            null);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println(">>> [JWT FILTER] User authenticated successfully via JWT: " + userEmail);
                }
            }
        } catch (Exception e) {
            // Token invalid or expired
            System.err.println(">>> [JWT FILTER] Exception processing token: " + e.getMessage());
        }

        // --- DEBUG LOG 4: Chain Continuation ---
        System.out.println(">>> [JWT FILTER] Proceeding with filter chain...");
        filterChain.doFilter(request, response);
    }
}
```

- in backend/src/main/java/com/fractal/backend/service/JwtService.java

```
package com.fractal.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;
import com.fractal.backend.service.WorkspaceService;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_ShouldSaveWorkspaceAndAddOwner() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@fractal.com");

        String workspaceName = "Fractal Inc";

        // Mock checking for slug uniqueness (return false means slug is not taken)
        when(workspaceRepository.existsBySlug(anyString())).thenReturn(false);

        // Mock saving workspace (return what passed in)
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> {
            Workspace w = i.getArgument(0);
            w.setId(UUID.randomUUID()); // Simulate DB ID generation
            return w;
        });

        // Act
        Workspace createdWorkspace = workspaceService.createWorkspace(user.getId(), workspaceName);

        // Assert
        assertThat(createdWorkspace).isNotNull();
        assertThat(createdWorkspace.getName()).isEqualTo(workspaceName);
        assertThat(createdWorkspace.getOwnerId()).isEqualTo(user.getId());
        assertThat(createdWorkspace.getSlug()).startsWith("fractal-inc");

        // Verify that the user was added as a member with OWNER role
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());

        WorkspaceMember member = memberCaptor.getValue();
        assertThat(member.getUserId()).isEqualTo(user.getId());
        assertThat(member.getRole()).isEqualTo("OWNER");
    }

    @Test
    void removeMember_OwnerRemovesMember_ShouldSucceed() {
        // Arrange
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        // Mock Requester (Owner)
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .userId(ownerId).workspaceId(workspaceId).role("OWNER").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                .thenReturn(Optional.of(ownerMember));

        // Mock Target (Member)
        WorkspaceMember targetMember = WorkspaceMember.builder()
                .userId(memberId).workspaceId(workspaceId).role("MEMBER").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId))
                .thenReturn(Optional.of(targetMember));

        // Act
        workspaceService.removeMember(ownerId, workspaceId, memberId);

        // Assert
        verify(workspaceMemberRepository).delete(targetMember);
    }

    @Test
    void removeMember_AdminRemovesOwner_ShouldThrowForbidden() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        // Mock Requester (Admin)
        WorkspaceMember adminMember = WorkspaceMember.builder()
                .userId(adminId).workspaceId(workspaceId).role("ADMIN").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, adminId))
                .thenReturn(Optional.of(adminMember));

        // Mock Target (Owner)
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .userId(ownerId).workspaceId(workspaceId).role("OWNER").build();
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                .thenReturn(Optional.of(ownerMember));

        // Act & Assert
        try {
            workspaceService.removeMember(adminId, workspaceId, ownerId);
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        }

        verify(workspaceMemberRepository, never()).delete(any());
    }
}
```

then in pom.xml:

````
  <dependency>
            <groupId>com.sendgrid</groupId>
            <artifactId>sendgrid-java</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        ```

- in backend/src/main/java/com/fractal/backend/dto/InviteMemberRequest.java
```
package com.fractal.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank
    @Email
    private String email;

    private String role = "MEMBER";
}
```

- in backend/src/main/java/com/fractal/backend/dto/UpdateWorkspaceRequest.java
```
package com.fractal.backend.dto;

import lombok.Data;

@Data
public class UpdateWorkspaceRequest {
    private String name;
    private String slug;
}
```

then i created the project routes..
- in backend/src/main/resources/db/migration/V3__add_projects.sql

```sql
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
```

- in backend/src/main/java/com/fractal/backend/dto/AddProjectMemberRequest.java
```
package com.fractal.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddProjectMemberRequest {
    @NotNull
    private UUID userId;
    private String role = "VIEWER";
}
```

- in backend/src/main/java/com/fractal/backend/dto/CreateProjectRequest.java
```
package com.fractal.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String color;
    private UUID parentId;
}
```

- in backend/src/main/java/com/fractal/backend/dto/ProjectMemberDTO.java
```
package com.fractal.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectMemberDTO {
    private UUID userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private OffsetDateTime joinedAt;
}
```

- in backend/src/main/java/com/fractal/backend/dto/ProjectResponse.java
```
package com.fractal.backend.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectResponse {
    private UUID id;
    private String name;
    private String color;
    private UUID parentId;
    private String role;
    private boolean isArchived;
}
```

- in backend/src/main/java/com/fractal/backend/dto/TransferProjectOwnershipRequest.java
```
package com.fractal.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferProjectOwnershipRequest {
    @NotNull
    private UUID newOwnerId;
}
```

- in backend/src/main/java/com/fractal/backend/dto/UpdateProjectMemberRequest.java
```
package com.fractal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProjectMemberRequest {
    @NotBlank
    private String role; // 'ADMIN', 'EDITOR', 'VIEWER'
}
```

- in backend/src/main/java/com/fractal/backend/model/Project.java
```
package com.fractal.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private String name;

    private String color;

    @Column(name = "is_archived")
    private boolean isArchived;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
```

- in backend/src/main/java/com/fractal/backend/model/ProjectMember.java
```
package com.fractal.backend.model;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProjectMember.ProjectMemberId.class)
public class ProjectMember {

    @Id
    @Column(name = "project_id")
    private UUID projectId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String role; // OWNER, ADMIN, EDITOR, VIEWER

    @Column(name = "is_favorite")
    private boolean isFavorite;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectMemberId implements Serializable {
        private UUID projectId;
        private UUID userId;
    }
}
```

- in backend/src/main/java/com/fractal/backend/repository/ProjectMemberRepository.java
```
package com.fractal.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fractal.backend.dto.ProjectMemberDTO;
import com.fractal.backend.model.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMember.ProjectMemberId> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findAllByProjectId(UUID projectId);

    // Bulk delete for cascading removal of a user from sub-projects
    @Modifying
    @Query("DELETE FROM ProjectMember pm WHERE pm.userId = :userId AND pm.projectId IN :projectIds")
    void deleteAllByUserIdAndProjectIdIn(UUID userId, List<UUID> projectIds);

    // Fetch members DTO
    @Query("SELECT new com.fractal.backend.dto.ProjectMemberDTO(u.id, u.email, u.fullName, u.avatarUrl, pm.role, pm.createdAt) "
            +
            "FROM ProjectMember pm JOIN User u ON pm.userId = u.id " +
            "WHERE pm.projectId = :projectId")
    List<ProjectMemberDTO> findMembersWithDetails(UUID projectId);
}
```

- in backend/src/main/java/com/fractal/backend/repository/ProjectRepository.java
```
package com.fractal.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT p FROM Project p " +
            "JOIN ProjectMember pm ON p.id = pm.projectId " +
            "WHERE p.workspaceId = :workspaceId AND pm.userId = :userId " +
            "AND p.deletedAt IS NULL")
    List<Project> findAllByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByIdAndWorkspaceId(UUID id, UUID workspaceId);

    // --- Closure Table Logic ---

    // 1. Insert Self Reference (depth 0)
    @Modifying
    @Query(value = "INSERT INTO project_hierarchy (ancestor_id, descendant_id, depth) VALUES (:projectId, :projectId, 0)", nativeQuery = true)
    void insertSelfReference(UUID projectId);

    // 2. Insert Hierarchy (Copy paths from parent)
    @Modifying
    @Query(value = """
                INSERT INTO project_hierarchy (ancestor_id, descendant_id, depth)
                SELECT ancestor_id, :descendantId, depth + 1
                FROM project_hierarchy
                WHERE descendant_id = :parentId
            """, nativeQuery = true)
    void insertHierarchy(UUID parentId, UUID descendantId);

    // 3. Find IDs for Cascade Delete (Including Self)
    @Query(value = """
                SELECT descendant_id FROM project_hierarchy
                WHERE ancestor_id = :projectId
            """, nativeQuery = true)
    List<UUID> findAllDescendantIdsIncludingSelf(UUID projectId);

    // 4. Find IDs for Member Cascade (Excluding Self usually, but here generally
    // descendants)
    @Query(value = """
                SELECT descendant_id FROM project_hierarchy
                WHERE ancestor_id = :projectId AND depth > 0
            """, nativeQuery = true)
    List<UUID> findAllDescendantIds(UUID projectId);
}
```

- in backend/src/test/java/com/fractal/service/ProjectServiceTest.java
```
package com.fractal.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.model.Project;
import com.fractal.backend.model.ProjectMember;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.ProjectMemberRepository;
import com.fractal.backend.repository.ProjectRepository;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.service.ProjectService;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

        @Mock
        private ProjectRepository projectRepository;
        @Mock
        private ProjectMemberRepository projectMemberRepository;
        @Mock
        private WorkspaceMemberRepository workspaceMemberRepository;
        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private ProjectService projectService;

        // ==================================================================================
        // 1. CREATE PROJECT TESTS
        // ==================================================================================

        @Test
        @DisplayName("createProject - Should succeed for a valid root project")
        void createProject_ShouldSucceedForRootProject() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                Project project = Project.builder().id(UUID.randomUUID()).build();

                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                when(projectRepository.save(any(Project.class))).thenReturn(project);

                // Act
                Project result = projectService.createProject(userId, workspaceId, "Test", "#FFF", null);

                // Assert
                assertThat(result).isNotNull();
                verify(projectRepository).insertSelfReference(project.getId());
                verify(projectRepository, never()).insertHierarchy(any(), any()); // No parent
                verify(projectMemberRepository).save(any(ProjectMember.class)); // Creator is owner
        }

        @Test
        @DisplayName("createProject - Should throw FORBIDDEN when user is not in workspace")
        void createProject_ShouldThrowForbidden_WhenNotInWorkspace() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.createProject(userId, workspaceId, "Test", null, null));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("createProject - Should inherit members for a sub-project")
        void createProject_ShouldInheritMembersForSubProject() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();
                UUID parentId = UUID.randomUUID();
                Project project = Project.builder().id(UUID.randomUUID()).build();
                ProjectMember parentMember = ProjectMember.builder().userId(UUID.randomUUID()).role("EDITOR").build();

                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                when(projectRepository.existsByIdAndWorkspaceId(parentId, workspaceId)).thenReturn(true);
                when(projectMemberRepository.findByProjectIdAndUserId(parentId, userId))
                                .thenReturn(Optional.of(new ProjectMember()));
                when(projectRepository.save(any(Project.class))).thenReturn(project);
                when(projectMemberRepository.findAllByProjectId(parentId)).thenReturn(List.of(parentMember));

                // Act
                projectService.createProject(userId, workspaceId, "Sub-project", null, parentId);

                // Assert
                verify(projectRepository).insertHierarchy(parentId, project.getId());
                verify(projectMemberRepository).saveAll(any()); // Verify inheritance was called
        }

        // ==================================================================================
        // 2. DELETE PROJECT TESTS
        // ==================================================================================

        @Test
        @DisplayName("deleteProject - Should cascade soft delete to all descendants")
        void deleteProject_ShouldCascadeSoftDelete() {
                // Arrange
                UUID userId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID childId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                Project project = Project.builder().id(projectId).workspaceId(workspaceId).build();
                Project childProject = Project.builder().id(childId).workspaceId(workspaceId).build();
                List<UUID> descendantIds = List.of(projectId, childId);

                // Mock permission checks
                when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                                .thenReturn(Optional.of(WorkspaceMember.builder().role("MEMBER").build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("OWNER").build()));

                // Mock the cascade logic
                when(projectRepository.findAllDescendantIdsIncludingSelf(projectId)).thenReturn(descendantIds);
                when(projectRepository.findAllById(descendantIds)).thenReturn(List.of(project, childProject));

                // Act
                projectService.deleteProject(userId, projectId);

                // Assert
                ArgumentCaptor<List<Project>> captor = ArgumentCaptor.forClass(List.class);
                verify(projectRepository).saveAll(captor.capture());

                List<Project> savedProjects = captor.getValue();
                assertThat(savedProjects).hasSize(2);
                assertThat(savedProjects.stream().allMatch(p -> p.getDeletedAt() != null)).isTrue();
        }

        // ==================================================================================
        // 3. MEMBER MANAGEMENT TESTS
        // ==================================================================================

        @Test
        @DisplayName("addMember - Should throw CONFLICT if user is already a member")
        void addMember_ShouldThrowConflictIfAlreadyMember() {
                // Arrange
                UUID requesterId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID newUserId = UUID.randomUUID();

                when(projectRepository.findById(projectId))
                                .thenReturn(Optional.of(Project.builder().workspaceId(UUID.randomUUID()).build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("ADMIN").build()));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(any(), any()))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                // This is the key mock for this test
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, newUserId))
                                .thenReturn(Optional.of(new ProjectMember()));

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.addMember(requesterId, projectId, newUserId, "EDITOR"));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("removeMember - Should cascade remove from all descendant projects")
        void removeMember_ShouldCascadeRemoveFromDescendants() {
                // Arrange
                UUID requesterId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID targetUserId = UUID.randomUUID();
                UUID childProjectId = UUID.randomUUID();
                List<UUID> descendantIds = List.of(childProjectId);

                ProjectMember targetMember = ProjectMember.builder().userId(targetUserId).role("EDITOR").build();

                // Mock permission checks
                when(projectRepository.findById(projectId))
                                .thenReturn(Optional.of(Project.builder().workspaceId(UUID.randomUUID()).build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("ADMIN").build()));

                when(projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId))
                                .thenReturn(Optional.of(targetMember));
                when(projectRepository.findAllDescendantIds(projectId)).thenReturn(descendantIds);

                // Act
                projectService.removeMember(requesterId, projectId, targetUserId);

                // Assert
                verify(projectMemberRepository).delete(targetMember); // Removed from parent
                verify(projectMemberRepository).deleteAllByUserIdAndProjectIdIn(targetUserId, descendantIds); // Cascade
                                                                                                              // removed
                                                                                                              // from
                                                                                                              // children
        }

        @Test
        @DisplayName("removeMember - Should throw FORBIDDEN when trying to remove the owner")
        void removeMember_ShouldThrowForbiddenWhenRemovingOwner() {
                // Arrange
                UUID requesterId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID targetUserId = UUID.randomUUID();
                ProjectMember ownerMember = ProjectMember.builder().userId(targetUserId).role("OWNER").build();

                when(projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId))
                                .thenReturn(Optional.of(ownerMember));

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.removeMember(requesterId, projectId, targetUserId));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        // ==================================================================================
        // 4. OWNERSHIP TRANSFER TESTS
        // ==================================================================================

        @Test
        @DisplayName("transferOwnership - Should succeed and swap roles correctly")
        void transferOwnership_ShouldSucceedAndSwapRoles() {
                // Arrange
                UUID ownerId = UUID.randomUUID();
                UUID newOwnerId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();
                UUID workspaceId = UUID.randomUUID();

                Project project = Project.builder().id(projectId).workspaceId(workspaceId).build();
                ProjectMember currentOwner = ProjectMember.builder().projectId(projectId).userId(ownerId).role("OWNER")
                                .build();
                ProjectMember newOwner = ProjectMember.builder().projectId(projectId).userId(newOwnerId).role("EDITOR")
                                .build();

                // Mock permission checks
                when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                                .thenReturn(Optional.of(new WorkspaceMember()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, ownerId))
                                .thenReturn(Optional.of(currentOwner));

                // Mock the logic
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, newOwnerId))
                                .thenReturn(Optional.of(newOwner));
                when(projectMemberRepository.findAllByProjectId(projectId)).thenReturn(List.of(currentOwner, newOwner));

                // Act
                projectService.transferOwnership(ownerId, projectId, newOwnerId);

                // Assert
                ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
                verify(projectMemberRepository, times(2)).save(captor.capture());

                List<ProjectMember> savedMembers = captor.getAllValues();
                ProjectMember demotedOwner = savedMembers.stream().filter(m -> m.getUserId().equals(ownerId))
                                .findFirst().get();
                ProjectMember promotedOwner = savedMembers.stream().filter(m -> m.getUserId().equals(newOwnerId))
                                .findFirst().get();

                assertThat(demotedOwner.getRole()).isEqualTo("ADMIN");
                assertThat(promotedOwner.getRole()).isEqualTo("OWNER");
        }

        @Test
        @DisplayName("transferOwnership - Should fail if new owner is not a project member")
        void transferOwnership_ShouldFailIfNewOwnerNotMember() {
                // Arrange
                UUID ownerId = UUID.randomUUID();
                UUID newOwnerId = UUID.randomUUID();
                UUID projectId = UUID.randomUUID();

                // Mock permission check to pass
                when(projectRepository.findById(projectId))
                                .thenReturn(Optional.of(Project.builder().workspaceId(UUID.randomUUID()).build()));
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, ownerId))
                                .thenReturn(Optional.of(ProjectMember.builder().role("OWNER").build()));
                when(workspaceMemberRepository.findByWorkspaceIdAndUserId(any(), any()))
                                .thenReturn(Optional.of(new WorkspaceMember()));

                // Mock new owner not being found
                when(projectMemberRepository.findByProjectIdAndUserId(projectId, newOwnerId))
                                .thenReturn(Optional.empty());

                // Act & Assert
                var exception = assertThrows(ResponseStatusException.class,
                                () -> projectService.transferOwnership(ownerId, projectId, newOwnerId));
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
}
```

- in backend/src/test/java/com/fractal/backend/controller/ProjectControllerTest.java
```
package com.fractal.backend.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fractal.backend.dto.AddProjectMemberRequest;
import com.fractal.backend.dto.CreateProjectRequest;
import com.fractal.backend.dto.ProjectMemberDTO;
import com.fractal.backend.dto.ProjectResponse;
import com.fractal.backend.dto.TransferProjectOwnershipRequest;
import com.fractal.backend.dto.UpdateProjectMemberRequest;
import com.fractal.backend.model.Project;
import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.security.JwtAuthenticationFilter;
import com.fractal.backend.service.JwtService;
import com.fractal.backend.service.ProjectService;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProjectService projectService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserRepository userRepository;

        private UUID userId;
        private UUID workspaceId;
        private UUID projectId;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                workspaceId = UUID.randomUUID();
                projectId = UUID.randomUUID();

                // 1. Manually set the Authentication Principal to our custom User entity
                // This is crucial because @WithMockUser creates a Spring UserDetails, not your
                // entity
                User mockUser = User.builder().id(userId).email("test@fractal.com").build();
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(mockUser, null,
                                Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
        }

        private User setupMockUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail("test@fractal.com");

                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
                return user;
        }

        // ==================================================================================
        // 1. CREATE PROJECT POST /api/workspaces/{workspaceId}/projects
        // ==================================================================================

        @Test
        @DisplayName("Create Project - Success")
        void createProject_Success() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName("New Project");
                request.setColor("#000000");

                Project mockProject = Project.builder().id(projectId).name("New Project").build();

                when(projectService.createProject(eq(userId), eq(workspaceId), eq("New Project"), eq("#000000"), any()))
                                .thenReturn(mockProject);

                mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(projectId.toString()))
                                .andExpect(jsonPath("$.role").value("OWNER"));
        }

        @Test
        @DisplayName("Create Project - 400 Bad Request (Validation Name Blank)")
        void createProject_ValidationFail() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName(""); // Invalid @NotBlank

                mockMvc.perform(post("/api/workspaces/{workspaceId}/projects", workspaceId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest()); // Verifies @Valid works
        }

        // ==================================================================================
        // 2. GET PROJECTS GET /api/workspaces/{workspaceId}/projects
        // ==================================================================================

        @Test
        @DisplayName("Get Projects - Success")
        void getProjects_Success() throws Exception {
                ProjectResponse response = ProjectResponse.builder().id(projectId).name("Demo").build();
                when(projectService.getProjects(userId, workspaceId)).thenReturn(List.of(response));

                mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));
        }

        // ==================================================================================
        // 3. UPDATE PROJECT PUT /api/projects/{projectId}
        // ==================================================================================

        @Test
        @DisplayName("Update Project - Success")
        void updateProject_Success() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName("Updated Name");
                request.setColor("#FFFFFF");

                Project mockProject = Project.builder().id(projectId).name("Updated Name").build();
                when(projectService.updateProject(userId, projectId, "Updated Name", "#FFFFFF"))
                                .thenReturn(mockProject);

                mockMvc.perform(put("/api/projects/{projectId}", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Name"));
        }

        @Test
        @DisplayName("Update Project - 403 Forbidden (Not Admin)")
        void updateProject_Forbidden() throws Exception {
                CreateProjectRequest request = new CreateProjectRequest();
                request.setName("Hacked");

                when(projectService.updateProject(any(), any(), any(), any()))
                                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                "Insufficient permissions"));

                mockMvc.perform(put("/api/projects/{projectId}", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        // ==================================================================================
        // 4. DELETE PROJECT DELETE /api/projects/{projectId}
        // ==================================================================================

        @Test
        @DisplayName("Delete Project - Success")
        void deleteProject_Success() throws Exception {
                mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(projectService).deleteProject(userId, projectId);
        }

        @Test
        @DisplayName("Delete Project - 404 Not Found")
        void deleteProject_NotFound() throws Exception {
                doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                                .when(projectService).deleteProject(userId, projectId);

                mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        // ==================================================================================
        // 5. GET MEMBERS GET /api/projects/{projectId}/members
        // ==================================================================================

        @Test
        @DisplayName("Get Members - Success")
        void getMembers_Success() throws Exception {
                ProjectMemberDTO member = new ProjectMemberDTO(userId, "email", "name", "url", "OWNER", null);
                when(projectService.getProjectMembers(userId, projectId)).thenReturn(List.of(member));

                mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].role").value("OWNER"));
        }

        // ==================================================================================
        // 6. ADD MEMBER POST /api/projects/{projectId}/members
        // ==================================================================================

        @Test
        @DisplayName("Add Member - Success")
        void addMember_Success() throws Exception {
                AddProjectMemberRequest request = new AddProjectMemberRequest();
                request.setUserId(UUID.randomUUID());
                request.setRole("EDITOR");

                mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(projectService).addMember(eq(userId), eq(projectId), eq(request.getUserId()), eq("EDITOR"));
        }

        @Test
        @DisplayName("Add Member - 400 Bad Request (Missing UserID)")
        void addMember_ValidationFail() throws Exception {
                AddProjectMemberRequest request = new AddProjectMemberRequest();
                request.setRole("EDITOR");
                // UserId is null

                mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Add Member - 409 Conflict (Already Member)")
        void addMember_Conflict() throws Exception {
                UUID userId = UUID.randomUUID();
                AddProjectMemberRequest request = new AddProjectMemberRequest();
                request.setUserId(userId);
                request.setRole("EDITOR"); // Explicitly set the role
                setupMockUser(userId);

                doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member"))
                                .when(projectService).addMember(
                                                eq(userId), // The user ID from the security context
                                                eq(projectId), // The project ID from the path
                                                eq(request.getUserId()), // The user ID from the request body
                                                eq(request.getRole()) // The role from the request body
                                );

                mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                                // csrf() is not needed when addFilters=false
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict()); // Now correctly expects and gets 409
        }

        // ==================================================================================
        // 7. UPDATE MEMBER ROLE PUT /api/projects/{projectId}/members/{userId}
        // ==================================================================================

        @Test
        @DisplayName("Update Member Role - Success")
        void updateMemberRole_Success() throws Exception {
                UpdateProjectMemberRequest request = new UpdateProjectMemberRequest();
                request.setRole("ADMIN");
                UUID targetUser = UUID.randomUUID();

                mockMvc.perform(put("/api/projects/{projectId}/members/{targetUser}", projectId, targetUser)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(projectService).updateMemberRole(userId, projectId, targetUser, "ADMIN");
        }

        @Test
        @DisplayName("Update Member Role - 400 Bad Request (Blank Role)")
        void updateMemberRole_ValidationFail() throws Exception {
                UpdateProjectMemberRequest request = new UpdateProjectMemberRequest();
                request.setRole(""); // Blank

                mockMvc.perform(put("/api/projects/{projectId}/members/{userId}", projectId, UUID.randomUUID())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // ==================================================================================
        // 8. REMOVE MEMBER DELETE /api/projects/{projectId}/members/{userId}
        // ==================================================================================

        @Test
        @DisplayName("Remove Member - Success")
        void removeMember_Success() throws Exception {
                UUID targetUser = UUID.randomUUID();

                mockMvc.perform(delete("/api/projects/{projectId}/members/{targetUser}", projectId, targetUser)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(projectService).removeMember(userId, projectId, targetUser);
        }

        @Test
        @DisplayName("Remove Member - 403 Forbidden (Trying to remove Owner)")
        void removeMember_Forbidden() throws Exception {
                UUID targetUser = UUID.randomUUID();
                doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove owner"))
                                .when(projectService).removeMember(userId, projectId, targetUser);

                mockMvc.perform(delete("/api/projects/{projectId}/members/{targetUser}", projectId, targetUser)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        // ==================================================================================
        // 9. TRANSFER OWNERSHIP POST /api/projects/{projectId}/transfer-ownership
        // ==================================================================================

        @Test
        @DisplayName("Transfer Ownership - Success")
        void transferOwnership_Success() throws Exception {
                TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
                request.setNewOwnerId(UUID.randomUUID());

                mockMvc.perform(post("/api/projects/{projectId}/transfer-ownership", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Transfer Ownership - 400 Bad Request (Null Owner ID)")
        void transferOwnership_ValidationFail() throws Exception {
                TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
                // newOwnerId is null

                mockMvc.perform(post("/api/projects/{projectId}/transfer-ownership", projectId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}
```

Till this point everything is done and implemented by me. i want your help after this point.

Run spring boot using: ./mvnw spring-boot:run

I want you to complete the backend for projects now.. I believe all the apis for workspace is done... basically all operations like inviting people to workspace, CRUD operations like updating the name, deleting the workspace etc. accoring to the original plan is done.. now i want to move to project level. - **Infinite Nesting:** Allow for infinite nesting of projects (sub-projects) and tasks (sub-tasks) using closure tables with `Ancestor`, `Descendant`, and `Depth` columns. this part.. so I need apis so that user can create delete update read projects in a workspace. he can infinitely create sub projects in a projects.. I will use closure tables for this. Also on the project level there will be 4 roles: "OWNER" (the one who creates, also has delete and transfer ownership permission) , "ADMIN" all permission, editor (can create and edit tasks in a project), and viewer (he can only view the tasks and subprojects in the project if he is member of a subproject),

I am also thinking..by default when a new sub project is created by an admin/owner, all the current admin and owners are members of it unless specified who to choose. Is this a good UX? what is the industry default? should i simply have 1 user set for all the projects and subprojects or different usersets for every subproject?? please research how this is currently done and what the best user experience is.

Once you have finalized the UX..Please go step by step for the backend. make sure I am following the industry level best practices. Make sure to write the test for every API you create.. dont miss any apis (must).

````
