A comprehensive and well-structured plan for a feature-rich Todo List application.

### **Advanced Features**

*   **Natural Language Task Addition:** Employ Retrieval-Augmented Generation (RAG) to convert natural language input into tasks. This will involve creating embeddings from the tasks and using a vector database like Qdrant with HNSW ANNS cosine similarity for searching.
*   **Email to Task:** Provide a dedicated email address that, upon receiving a forwarded email, automatically creates a new task.
*   **Browser Extension:** A browser extension to capture the current webpage as a task, ideal for creating reading lists or research items.
*   **Bi-Directional API / Webhooks:** Instead of relying on pre-built integrations, offer a webhook URL for each project. This empowers power users to connect with services like IFTTT, Zapier, or custom scripts (e.g., creating a task when a GitHub issue is assigned).
*   **Markdown and Rich Text Support:** The task description field will fully support Markdown, including headers, code blocks, bold, italics, and inline image rendering.
*   **File Attachments:** Allow users to attach PDFs, images, or spreadsheets directly to tasks for reference.
*   **Undo/Redo History:** Implement infinite undo/redo functionality for all changes within a list. Accidental project deletions can be reversed with a simple shortcut. This will be managed through a `command_log` table storing each user event.
*   **Drag and Drop Functionality:** Utilize fractional indexing with an algorithm like LexoRank to enable smooth and persistent drag-and-drop ordering of tasks.
*   **Local-First Application:** Design the app to work offline using Conflict-free Replicated Data Types (CRDTs) and Automerge for seamless data synchronization.
*   **Attribute-Based Access Control (ABAC):** Implement fine-grained permissions (READ, CREATE, UPDATE, DELETE, COMPLETE, RESCHEDULE) using Open Policy Agent (OPA) with policies written in Rego.
*   **Role-Based Access Control (RBAC):** A hierarchical permission system with roles: Owner > Admin > Editor > Viewer.

### **Schedule Management**

*   **Start and End Dates:** Define start dates and deadlines for tasks.
*   **Recurring Tasks:** Support for daily, weekly, and monthly recurrences based on the iCalendar (RFC 5545) standard, potentially using a library like ical4j in Java.
*   **Time Blocking:** Sync tasks with Google Calendar and Apple Calendar to facilitate time blocking.
*   **Timezone Intelligence:** Handle both fixed and floating timezones for tasks.

### **Structuring**

*   **Infinite Nesting:** Allow for infinite nesting of projects (sub-projects) and tasks (sub-tasks) using closure tables with `Ancestor`, `Descendant`, and `Depth` columns.
*   **Tags:** A flexible many-to-many relationship for tagging tasks (e.g., @Computer, #DeepWork, LowEnergy).
*   **Kanban & Gantt Views:** Provide the ability to visualize project tasks as a Kanban board or a Gantt chart.

### **Workflow & Execution**

*   **Task Dependencies:** Implement task dependencies where one task cannot be completed until another is finished, managed using a Directed Acyclic Graph (DAG) and topological sorting.
*   **Focus Mode:** A feature to hide all UI distractions, showing only the current task.
*   **Pomodoro Timer Integration:** A built-in Pomodoro timer to help users focus on tasks for 25-minute intervals.
*   **Templates:** Allow users to save a list of tasks as a template (e.g., "New Client Onboarding") for reuse.
*   **Task "Rot" & Auto-Archiving:** Visual cues for stale tasks, such as fading colors or icons. The app will prompt users to deal with tasks that have been repeatedly rescheduled.
*   **Eisenhower Matrix View:** A dynamic four-quadrant grid that automatically sorts tasks based on their urgency and importance.
*   **Soft vs. Hard Deadlines:** Differentiate between hard deadlines (must be done) and soft, goal-oriented deadlines that don't trigger "overdue" alerts if missed by a short period.
*   **Notification System:** A system to send reminders and notifications to users.

### **Collaboration**

*   **Shared Projects with Permissions:** Invite others to projects with specific permissions (Read Only, Can Edit, Can Admin).
*   **Task Assignment:** Assign tasks within a shared project to specific individuals.
*   **Comments & Activity Log:** A discussion thread within each task and a log of all changes.

### **AI Features**

*   **Task Breakdown:** An AI feature to break down a large task into smaller, manageable sub-tasks.
*   **Smart Rescheduling:** An AI algorithm to analyze overdue tasks and suggest a realistic new schedule based on priorities and calendar availability.
*   **Context-Aware Suggestions:** The app will suggest quick tasks to complete based on the user's available time slots.

### **Security**

*   **Data Encryption:** All data at rest will be encrypted.
*   **Compliance:** Adherence to security standards such as SOC 2, GDPR, and ISO.

### **Analytics**

*   **Productivity Pulse/Velocity:** A visual representation of task completion rates over time.
*   **Estimated vs. Actual Time:** The app will track the difference between estimated and actual time spent on tasks to provide insights into estimation accuracy.
*   **Circadian Rhythm Optimization:** The app will learn the user's most productive times and suggest scheduling deep work accordingly.
*   **CQRS Pattern:** Utilize the Command Query Responsibility Segregation pattern for managing data.

### **Tech Stack**

*   **Frontend:** Next.js (initially without local-first, to be added later).
*   **Backend:** Spring Boot (Java).
*   **Scalability:** The application will be designed to handle over 200 million requests.
*   **AI:** AWS Bedrock for embedding models.
*   **Vector Database:** Qdrant (free tier).
*   **Event Logging:** Initially a `command_log` table, with plans to migrate to Kafka.
*   **Database:** Writes (Commands) and initial Reads will go to a normalized PostgreSQL database. There is potential to later shift read operations to OpenSearch.
*   **Development Methodology:** Test-Driven Development (TDD).
*   **Initial Architecture:** Serverless approach using AWS Lambda and API Gateway, with a future migration to a load balancer and ECS Fargate to mitigate cold starts.
*   **Authentication:** OAuth 2.0 with Google Sign-In as the initial provider.
*   **Database Hosting:** AWS RDS for PostgreSQL.
*   **Cold Start Mitigation:** AWS SnapStart will be used to reduce cold start times for Lambda functions.
- using redis for cacheing. 
- use flyway for database migrations

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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @GetMapping("/health")
    public String healthCheck() {
        return "System Operational";
    }

    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "This is a protected resource.";
    }
}
```

- in the backend/src/test/java/com/fractal/controller/HealthCheckControllerTest.java
```
package com.fractal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fractal.backend.config.SecurityConfig;
import com.fractal.backend.controller.HealthCheckController;
import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;

@WebMvcTest(HealthCheckController.class)
@Import(SecurityConfig.class)
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We mock this bean so SecurityConfig can load without needing the real
    // AuthService
    @MockitoBean
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isFound()); // 302 Redirect to Login
    }
}
```

- in the backend/src/main/resources/db/migration/V1__init_schema.sql i have added:
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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data // Lombok: Generates Getters, Setters, toString, etc.
@NoArgsConstructor
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
    private boolean isActive = true;

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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.fractal.backend.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // Add this for constructor injection
public class SecurityConfig {

    // Inject your custom success handler
    private final CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 0. Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // 1. Authorize Requests
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/api/**").permitAll() // Allow
                                                                                                             // OPTIONS
                                                                                                             // for CORS
                                                                                                             // preflight
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // 2. Configure OAuth2 Login
                .oauth2Login(oauth2 -> {
                    // Tell Spring Security to use your custom success handler
                    oauth2.successHandler(customOAuth2AuthenticationSuccessHandler);
                })

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000", // Keep localhost for safety
                "http://lvh.me:3000", // Main domain
                "http://*.lvh.me:3000" // All subdomains
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        configuration.setAllowCredentials(true); // Allow credentials (cookies, authorization headers)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration); // Apply CORS to /api paths
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

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public WorkspaceResponse createWorkspace(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWorkspaceRequest request) {

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        // Call the service
        Workspace workspace = workspaceService.createWorkspace(user.getId(), request.getName());

        // Return the response
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .role("OWNER")
                .build();
    }

    @GetMapping
    public List<WorkspaceResponse> getUserWorkspaces(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return workspaceService.getWorkspacesForUser(user.getId());
    }
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
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fractal.backend.model.WorkspaceMember;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMember.WorkspaceMemberId> {
    List<WorkspaceMember> findAllByUserId(UUID userId);
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.model.WorkspaceMember;
import com.fractal.backend.repository.WorkspaceMemberRepository;
import com.fractal.backend.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public Workspace createWorkspace(UUID userId, String name) {
        // 1. Generate a slug (URL friendly version of name)
        String slug = toSlug(name);

        // 2. Ensure slug uniqueness (simple append strategy)
        String originalSlug = slug;
        int count = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + count;
            count++;
        }

        // 3. Create Workspace
        Workspace workspace = Workspace.builder()
                .ownerId(userId)
                .name(name)
                .slug(slug)
                .planType("FREE")
                .build();

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // 4. Add Creator as OWNER
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(savedWorkspace.getId())
                .userId(userId)
                .role("OWNER")
                .build();

        workspaceMemberRepository.save(member);

        return savedWorkspace;
    }

    // Helper to convert "My Company Name" -> "my-company-name"
    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public List<WorkspaceResponse> getWorkspacesForUser(UUID userId) {
        // 1. Find all memberships for this user
        List<WorkspaceMember> memberships = workspaceMemberRepository.findAllByUserId(userId);

        // 2. For each membership, fetch the Workspace details and map to DTO
        return memberships.stream()
                .map(member -> {
                    Workspace w = workspaceRepository.findById(member.getWorkspaceId()).orElse(null);
                    if (w == null)
                        return null;

                    return WorkspaceResponse.builder()
                            .id(w.getId())
                            .name(w.getName())
                            .slug(w.getSlug())
                            .role(member.getRole())
                            .build();
                })
                .filter(response -> response != null) // Filter out any nulls if workspace wasn't found
                .collect(Collectors.toList());
    }

}
```

- in backend/src/test/java/com/fractal/controller/WorkspaceControllerTest.java
```
package com.fractal.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fractal.backend.config.TestSecurityConfig;
import com.fractal.backend.controller.WorkspaceController;
import com.fractal.backend.dto.CreateWorkspaceRequest;
import com.fractal.backend.dto.WorkspaceResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.model.Workspace;
import com.fractal.backend.repository.UserRepository;
import com.fractal.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.fractal.backend.service.WorkspaceService;

@WebMvcTest(WorkspaceController.class)
@Import(TestSecurityConfig.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Use `new ObjectMapper()` directly instead of Autowiring if config is tricky
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WorkspaceService workspaceService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    @Test
    void createWorkspace_ShouldReturnCreatedWorkspace() throws Exception {
        // Arrange
        String email = "test@fractal.com";
        UUID userId = UUID.randomUUID();
        String workspaceName = "My New Company";

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);

        Workspace mockWorkspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name(workspaceName)
                .slug("my-new-company")
                .ownerId(userId)
                .build();

        // Mock DB finding the user by email
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // Mock Service creating the workspace
        when(workspaceService.createWorkspace(eq(userId), eq(workspaceName))).thenReturn(mockWorkspace);

        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName(workspaceName);

        // Act & Assert
        mockMvc.perform(post("/api/workspaces")
                .with(oauth2Login().attributes(attrs -> attrs.put("email", email)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))) // Use the manual instance
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(workspaceName))
                .andExpect(jsonPath("$.slug").value("my-new-company"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void getUserWorkspaces_ShouldReturnList() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("test@fractal.com");

        WorkspaceResponse ws1 = WorkspaceResponse.builder()
                .id(UUID.randomUUID()).name("WS 1").slug("ws-1").role("OWNER").build();

        // Mock the user finding (needed for the filter to set the
        // @AuthenticationPrincipal)
        when(userRepository.findByEmail("test@fractal.com")).thenReturn(Optional.of(mockUser));

        // Mock the service call
        when(workspaceService.getWorkspacesForUser(userId)).thenReturn(List.of(ws1));

        // Act & Assert
        mockMvc.perform(get("/api/workspaces")
                // We simulate the JWT auth by mocking the OAuth2 user,
                // OR if you are using the new JWT filter in tests, you might need to mock that
                // behavior.
                // For this WebMvcTest, simply injecting the user via security context is
                // easiest:
                .with(oauth2Login().attributes(attrs -> attrs.put("email", "test@fractal.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("WS 1"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
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

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "
        try {
            userEmail = jwtService.extractUsername(jwt);

            // 2. If user is found and not already authenticated in this context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3. Load user from DB
                // Ideally use a UserDetailsService, but we can fetch directly for now
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && jwtService.isTokenValid(jwt, user.getEmail())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            null);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalid or expired
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }
}
```

- in backend/src/main/java/com/fractal/backend/service/JwtService.java
```
package com.fractal.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Value("${JWT_EXPIRATION}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String email) {
        return generateToken(new HashMap<>(), email);
    }

    public String generateToken(Map<String, Object> extraClaims, String email) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String email) {
        final String username = extractUsername(token);
        return (username.equals(email)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```





google login is working. 

this is the current file structure is frontend
.
├── app
│   ├── (site)
│   │   ├── auth
│   │   │   └── callback
│   │   │       ├── loading.tsx
│   │   │       └── page.tsx
│   │   ├── login
│   │   │   └── page.tsx
│   │   ├── page.tsx
│   │   ├── select-workspace
│   │   │   └── page.tsx
│   │   └── welcome
│   │       └── new-workspace
│   │           └── page.tsx
│   ├── [domain]
│   │   ├── dashboard
│   │   │   └── page.tsx
│   │   └── page.tsx
│   ├── globals.css
│   └── layout.tsx
├── bun.lock
├── components
│   ├── auth
│   │   └── login-form.tsx
│   ├── dashboard
│   │   ├── dashboard-content.tsx
│   │   └── dashboard-layout.tsx
│   ├── theme-provider.tsx
│   ├── ui
│   │   ├── accordion.tsx
│   │   ├── alert-dialog.tsx
│   │   ├── alert.tsx
│   │   ├── aspect-ratio.tsx
│   │   ├── avatar.tsx
│   │   ├── badge.tsx
│   │   ├── breadcrumb.tsx
│   │   ├── button-group.tsx
│   │   ├── button.tsx
│   │   ├── calendar.tsx
│   │   ├── card.tsx
│   │   ├── carousel.tsx
│   │   ├── chart.tsx
│   │   ├── checkbox.tsx
│   │   ├── collapsible.tsx
│   │   ├── command.tsx
│   │   ├── context-menu.tsx
│   │   ├── dialog.tsx
│   │   ├── drawer.tsx
│   │   ├── dropdown-menu.tsx
│   │   ├── empty.tsx
│   │   ├── field.tsx
│   │   ├── form.tsx
│   │   ├── hover-card.tsx
│   │   ├── input-group.tsx
│   │   ├── input-otp.tsx
│   │   ├── input.tsx
│   │   ├── item.tsx
│   │   ├── kbd.tsx
│   │   ├── label.tsx
│   │   ├── menubar.tsx
│   │   ├── navigation-menu.tsx
│   │   ├── pagination.tsx
│   │   ├── popover.tsx
│   │   ├── progress.tsx
│   │   ├── radio-group.tsx
│   │   ├── resizable.tsx
│   │   ├── scroll-area.tsx
│   │   ├── select.tsx
│   │   ├── separator.tsx
│   │   ├── sheet.tsx
│   │   ├── sidebar.tsx
│   │   ├── skeleton.tsx
│   │   ├── slider.tsx
│   │   ├── sonner.tsx
│   │   ├── spinner.tsx
│   │   ├── switch.tsx
│   │   ├── table.tsx
│   │   ├── tabs.tsx
│   │   ├── textarea.tsx
│   │   ├── toast.tsx
│   │   ├── toaster.tsx
│   │   ├── toggle-group.tsx
│   │   ├── toggle.tsx
│   │   ├── tooltip.tsx
│   │   ├── use-mobile.tsx
│   │   └── use-toast.ts
│   └── workspace
│       └── workspace-selector.tsx
├── components.json
├── hooks
│   ├── use-mobile.ts
│   └── use-toast.ts
├── lib
│   ├── api.ts
│   ├── auth-context.tsx
│   ├── types.ts
│   └── utils.ts
├── next-env.d.ts
├── next.config.mjs
├── package.json
├── pnpm-lock.yaml
├── postcss.config.mjs
├── proxy.ts
├── public
│   ├── apple-icon.png
│   ├── icon-dark-32x32.png
│   ├── icon-light-32x32.png
│   ├── icon.svg
│   ├── placeholder-logo.png
│   ├── placeholder-logo.svg
│   ├── placeholder-user.jpg
│   ├── placeholder.jpg
│   └── placeholder.svg
├── styles
│   └── globals.css
└── tsconfig.json


- in frontend/app/(site)/auth/callback/page.tsx
```
"use client";

import { useEffect, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiClient } from "@/lib/api";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/lib/auth-context";

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const processedRef = useRef(false);
  const { refreshWorkspaces } = useAuth();

  useEffect(() => {
    const processLogin = async () => {
      // Prevent double execution in React Strict Mode
      if (processedRef.current) return;
      processedRef.current = true;

      // 1. Check for the token we sent from the backend
      const token = searchParams.get("token");

      if (!token) {
        // If we still have the old "error" param or no token
        const error = searchParams.get("error");
        console.error("Auth Error:", error || "No token found");
        router.push("/login?error=" + (error || "no_token"));
        return;
      }

      try {
        // 2. Set the token in our API client (saves to localStorage)
        apiClient.setAccessToken(token);

        // 3. Force a reload of the user state or just redirect
        // We will fetch the user data to decide routing
        // (Note: This call will fail until we do Step 2 below, but that's okay for now)
        try {
          const workspaces = await refreshWorkspaces();
          if (workspaces.length === 0) {
            router.replace("/welcome/new-workspace");
          } else {
            router.replace("/select-workspace");
          }
        } catch (e) {
          // If fetching fails, we default to dashboard or new-workspace
          // This allows us to proceed even if the /me endpoint isn't ready
          console.warn("Could not fetch workspaces, redirecting to default", e);
          router.replace("/select-workspace");
        }
      } catch (error) {
        console.error("Failed to process login", error);
        router.push("/login?error=auth_failed");
      }
    };

    processLogin();
  }, [router, searchParams]);

  return (
    <div className="flex h-screen w-full items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-muted-foreground">Authenticating...</p>
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <AuthCallbackContent />
    </Suspense>
  );
}

```

- in frontend/app/welcome/new-workspace/page.tsx
```
"use client"

import React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/auth-context"
import { apiClient } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { toast } from "sonner"
import { ArrowRight, Building2, Loader2, Sparkles } from "lucide-react"

export default function NewWorkspacePage() {
  const router = useRouter()
  const { user, refreshWorkspaces, setCurrentWorkspace } = useAuth()
  const [workspaceName, setWorkspaceName] = useState("")
  const [isCreating, setIsCreating] = useState(false)

  const generateSlug = (name: string) => {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "")
  }

  const handleCreateWorkspace = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!workspaceName.trim()) {
      toast.error("Please enter a workspace name")
      return
    }

    setIsCreating(true)

    try {
      const response = await apiClient.createWorkspace({ name: workspaceName })
      await refreshWorkspaces()
      setCurrentWorkspace(response.workspace)
      toast.success("Workspace created successfully!")
      router.push(response.redirectUrl || "/dashboard")
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to create workspace"
      )
    } finally {
      setIsCreating(false)
    }
  }

  const slug = generateSlug(workspaceName)

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      {/* Header */}
      <header className="border-b bg-background/80 backdrop-blur-sm">
        <div className="container mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-foreground flex items-center justify-center">
              <span className="text-background font-bold">T</span>
            </div>
            <span className="font-semibold">TaskFlow</span>
          </div>
          {user && (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>{user.email}</span>
            </div>
          )}
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-8">
          {/* Welcome message */}
          <div className="text-center space-y-2">
            <div className="flex items-center justify-center mb-4">
              <div className="h-16 w-16 rounded-2xl bg-foreground/5 flex items-center justify-center">
                <Sparkles className="h-8 w-8 text-foreground" />
              </div>
            </div>
            <h1 className="text-3xl font-bold tracking-tight">
              Create your first workspace
            </h1>
            <p className="text-muted-foreground max-w-md mx-auto">
              Workspaces are shared environments where you and your team can
              collaborate on tasks and projects.
            </p>
          </div>

          {/* Workspace form */}
          <Card className="border-0 shadow-lg">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Building2 className="h-5 w-5" />
                Workspace details
              </CardTitle>
              <CardDescription>
                Choose a name for your workspace. You can always change this
                later.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateWorkspace} className="space-y-6">
                <div className="space-y-2">
                  <Label htmlFor="workspace-name">Workspace name</Label>
                  <Input
                    id="workspace-name"
                    placeholder="Acme Inc."
                    value={workspaceName}
                    onChange={(e) => setWorkspaceName(e.target.value)}
                    className="h-12 text-base"
                    autoFocus
                  />
                </div>

                {slug && (
                  <div className="space-y-2">
                    <Label className="text-muted-foreground">
                      Workspace URL
                    </Label>
                    <div className="flex items-center gap-0 rounded-md border bg-muted/50 px-3 py-2">
                      <span className="text-sm text-muted-foreground">
                        taskflow.app/
                      </span>
                      <span className="text-sm font-medium">{slug}</span>
                    </div>
                  </div>
                )}

                <Button
                  type="submit"
                  className="w-full h-12 text-base"
                  disabled={!workspaceName.trim() || isCreating}
                >
                  {isCreating ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      Creating workspace...
                    </>
                  ) : (
                    <>
                      Create workspace
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </>
                  )}
                </Button>
              </form>
            </CardContent>
          </Card>

          {/* Tips */}
          <div className="bg-background border rounded-lg p-4 space-y-3">
            <h3 className="font-medium text-sm">Tips for workspace names</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-start gap-2">
                <span className="text-foreground">1.</span>
                Use your company or team name for easy recognition
              </li>
              <li className="flex items-start gap-2">
                <span className="text-foreground">2.</span>
                Keep it short and memorable for quick access
              </li>
              <li className="flex items-start gap-2">
                <span className="text-foreground">3.</span>
                You can create multiple workspaces for different projects
              </li>
            </ul>
          </div>
        </div>
      </main>
    </div>
  )
}
```

- in frontend/lib/auth-context.tsx
```
"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import { apiClient } from "./api";
import type { User, Workspace, AuthState } from "./types";
import { useRouter } from "next/navigation";

interface AuthContextType extends AuthState {
  login: () => void;
  logout: () => Promise<void>;
  setCurrentWorkspace: (workspace: Workspace) => void;
  refreshWorkspaces: () => Promise<Workspace[]>;
  handleAuthCallback: (code: string) => Promise<{ redirectUrl: string }>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [state, setState] = useState<AuthState>({
    user: null,
    workspaces: [],
    currentWorkspace: null,
    isLoading: true,
    isAuthenticated: false,
  });

  const refreshUser = useCallback(async () => {
    try {
      const [user, workspaces] = await Promise.all([
        apiClient.getCurrentUser(),
        apiClient.getUserWorkspaces(),
      ]);
      setState((prev) => ({
        ...prev,
        user,
        workspaces,
        isAuthenticated: true,
        isLoading: false,
      }));
    } catch {
      setState((prev) => ({
        ...prev,
        user: null,
        workspaces: [],
        isAuthenticated: false,
        isLoading: false,
      }));
    }
  }, []);

  useEffect(() => {
    if (apiClient.isAuthenticated()) {
      refreshUser();
    } else {
      setState((prev) => ({ ...prev, isLoading: false }));
    }
  }, [refreshUser]);

  const login = useCallback(() => {
    window.location.href = apiClient.getGoogleAuthUrl();
  }, []);

  const logout = useCallback(async () => {
    await apiClient.logout();
    setState({
      user: null,
      workspaces: [],
      currentWorkspace: null,
      isLoading: false,
      isAuthenticated: false,
    });
    router.replace("/");
  }, []);

  const setCurrentWorkspace = useCallback((workspace: Workspace) => {
    setState((prev) => ({ ...prev, currentWorkspace: workspace }));
    if (typeof window !== "undefined") {
      localStorage.setItem("currentWorkspaceId", workspace.id);
    }
  }, []);

  const refreshWorkspaces = useCallback(async () => {
    const workspaces = await apiClient.getUserWorkspaces();
    setState((prev) => ({ ...prev, workspaces }));
    return workspaces;
  }, []);

  const handleAuthCallback = useCallback(async (code: string) => {
    const response = await apiClient.handleOAuthCallback(code);
    setState((prev) => ({
      ...prev,
      user: response.user,
      workspaces: response.workspaces,
      isAuthenticated: true,
      isLoading: false,
    }));
    return { redirectUrl: response.redirectUrl };
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        setCurrentWorkspace,
        refreshWorkspaces,
        handleAuthCallback,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
```

- in frontend/app/(site)/select-workspace/page.tsx
```
"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Building2, ChevronRight, Plus } from "lucide-react";
import type { Workspace } from "@/lib/types";

export default function SelectWorkspacePage() {
  const router = useRouter();
  const { user, workspaces, logout } = useAuth();

  const handleSelectWorkspace = (workspace: Workspace) => {
    const rootDomain = process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
    const protocol = window.location.protocol;
    const targetUrl = `${protocol}//${workspace.slug}.${rootDomain}/dashboard`;
    window.location.href = targetUrl;
  };

  const handleCreateNew = () => {
    router.push("/welcome/new-workspace");
  };

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      {/* Header */}
      <header className="border-b bg-background/80 backdrop-blur-sm">
        <div className="container mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-foreground flex items-center justify-center">
              <span className="text-background font-bold">T</span>
            </div>
            <span className="font-semibold">TaskFlow</span>
          </div>
          <div className="flex items-center gap-4">
            {user && (
              <span className="text-sm text-muted-foreground">
                {user.email}
              </span>
            )}
            <Button variant="ghost" size="sm" onClick={handleLogout}>
              Sign out
            </Button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-lg space-y-6">
          {/* Header */}
          <div className="text-center space-y-2">
            <h1 className="text-3xl font-bold tracking-tight">
              Choose a workspace
            </h1>
            <p className="text-muted-foreground">
              Select a workspace to continue or create a new one
            </p>
          </div>

          {/* Workspace list */}
          <Card className="border-0 shadow-lg">
            <CardHeader>
              <CardTitle className="text-lg">Your workspaces</CardTitle>
              <CardDescription>
                You have access to {workspaces.length} workspace
                {workspaces.length !== 1 ? "s" : ""}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              {workspaces.map((workspace) => (
                <button
                  key={workspace.id}
                  onClick={() => handleSelectWorkspace(workspace)}
                  className="w-full flex items-center justify-between p-3 rounded-lg border bg-background hover:bg-accent transition-colors text-left group"
                >
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-foreground/5 flex items-center justify-center">
                      <Building2 className="h-5 w-5 text-foreground" />
                    </div>
                    <div>
                      <div className="font-medium">{workspace.name}</div>
                      <div className="text-sm text-muted-foreground">
                        {workspace.slug}.{window.location.host}
                      </div>
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground group-hover:text-foreground transition-colors" />
                </button>
              ))}
            </CardContent>
          </Card>

          {/* Create new workspace */}
          <Button
            variant="outline"
            className="w-full h-12 gap-2 bg-transparent"
            onClick={handleCreateNew}
          >
            <Plus className="h-4 w-4" />
            Create new workspace
          </Button>
        </div>
      </main>
    </div>
  );
}

```

- in frontend/app/[domain]/dashboard/page.tsx
```
"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { DashboardLayout } from "@/components/dashboard/dashboard-layout";
import { DashboardContent } from "@/components/dashboard/dashboard-content";
import { getSubdomain } from "@/lib/utils"; // Make sure you import this

export default function DashboardPage() {
  const router = useRouter();
  const {
    isAuthenticated,
    isLoading,
    workspaces,
    currentWorkspace,
    setCurrentWorkspace,
  } = useAuth();
  const [isWorkspaceResolved, setIsWorkspaceResolved] = useState(false);

  useEffect(() => {
    if (isLoading) return;

    if (!isAuthenticated) {
      const rootDomain =
        process.env.NEXT_PUBLIC_ROOT_DOMAIN || "localhost:3000";
      const protocol = window.location.protocol;
      window.location.href = `${protocol}//${rootDomain}/login`;
      return;
    }

    // --- NEW LOGIC START ---

    // 1. Get the subdomain from the current hostname
    // We can rely on window.location.hostname in the client
    const hostname = window.location.hostname;
    console.log("hostname", hostname);
    const subdomain = getSubdomain(hostname);
    console.log("subdomain", subdomain);
    if (subdomain) {
      console.log(subdomain);
      // 2. Find the workspace that matches this subdomain
      const matchingWorkspace = workspaces.find((w) => w.slug === subdomain);

      if (matchingWorkspace) {
        // 3. Set it as active if it's not already
        if (currentWorkspace?.id !== matchingWorkspace.id) {
          setCurrentWorkspace(matchingWorkspace);
        }
        setIsWorkspaceResolved(true);
      } else {
        // 4. If user has no access to this specific subdomain workspace -> 404 or redirect
        // For now, let's send them back to selection
        console.warn(`User does not have access to workspace: ${subdomain}`);
        return;
        router.push("/select-workspace"); // This will get rewritten to the root domain by middleware if configured, or just go to root
      }
    } else {
      // If we are somehow on the dashboard without a subdomain (should be impossible via middleware),
      console.log("no subdomain found");
      return;
      // send to select-workspace

      router.push("/select-workspace");
    }
    // --- NEW LOGIC END ---
  }, [
    isAuthenticated,
    isLoading,
    workspaces,
    currentWorkspace,
    setCurrentWorkspace,
    router,
  ]);

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-2">
          <div className="animate-pulse text-muted-foreground">
            Authenticating...
          </div>
        </div>
      </div>
    );
  }

  if (isLoading || !isWorkspaceResolved) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-2">
          <div className="animate-pulse text-muted-foreground">
            Loading Workspace...
          </div>
        </div>
      </div>
    );
  }

  return (
    <DashboardLayout>
      <DashboardContent />
    </DashboardLayout>
  );
}
```

- in frontend/lib/api.ts
```
import type {
  LoginResponse,
  CreateWorkspaceRequest,
  CreateWorkspaceResponse,
  User,
  Workspace,
} from "./types";
import Cookies from "js-cookie";
import { getCookieDomain } from "./utils";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class ApiClient {
  private accessToken: string | null = null;

  setAccessToken(token: string | null) {
    this.accessToken = token;
    if (token) {
      Cookies.set("accessToken", token, {
        expires: 7,
        domain: getCookieDomain(),
        sameSite: "Lax",
      });
    } else {
      this.logout();
    }
  }

  getAccessToken(): string | null {
    if (this.accessToken) return this.accessToken;
    const token = Cookies.get("accessToken");
    if (token) {
      this.accessToken = token;
      return token;
    }
    return null;
  }
  private async fetch<T>(
    endpoint: string,
    options: RequestInit = {},
  ): Promise<T> {
    const token = this.getAccessToken();
    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...options.headers,
    };

    if (token) {
      (headers as Record<string, string>)["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
      credentials: "omit",
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || `API Error: ${response.status}`);
    }

    return response.json();
  }

  // Auth endpoints
  getGoogleAuthUrl(): string {
    const redirectUri =
      typeof window !== "undefined"
        ? `${window.location.origin}/auth/callback`
        : "";
    return `${API_BASE_URL}/oauth2/authorization/google?redirect_uri=${encodeURIComponent(redirectUri)}`;
  }

  async handleOAuthCallback(code: string): Promise<LoginResponse> {
    const response = await this.fetch<LoginResponse>(
      "/api/auth/oauth/callback",
      {
        method: "POST",
        body: JSON.stringify({ code }),
      },
    );
    this.setAccessToken(response.accessToken);
    return response;
  }

  async getCurrentUser(): Promise<User> {
    return this.fetch<User>("/api/auth/me");
  }

  async getUserWorkspaces(): Promise<Workspace[]> {
    return this.fetch<Workspace[]>("/api/workspaces");
  }

  async createWorkspace(
    data: CreateWorkspaceRequest,
  ): Promise<CreateWorkspaceResponse> {
    return this.fetch<CreateWorkspaceResponse>("/api/workspaces", {
      method: "POST",
      body: JSON.stringify(data),
    });
  }

  logout() {
    this.accessToken = null;
    Cookies.remove("accessToken", { domain: getCookieDomain() });
    Cookies.remove("accessToken"); // Fallback cleanup
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}

export const apiClient = new ApiClient();
```

Till this point everything is done and implemented by me. i want your help after this point. 

Run spring boot using: ./mvnw spring-boot:run

2. Create a home page and login I want to setup google oauth 2 for login. I have setup the backend in spring boot using TDD. the signup flow should be like notion/slack, where user must first create a workspace if he has not done yet, if he has multiple workspaces, as i want to allow preview deployements to also be able to log in when i host this on vercel. also this is multitenant architecture.. so basically the user will go to workspace-name.domain.com.. You can use any component library for this. currently whatever workspace the user selects on frontend, he is redirected to /dashboard. i dont want this behaviour, the hostname should also change... like workspace-name.domain.com/dashboard.

i found this on github, maybe it will be helpful for your reference:
this is the middleware.ts 
```
import { type NextRequest, NextResponse } from 'next/server';
import { rootDomain } from '@/lib/utils';

function extractSubdomain(request: NextRequest): string | null {
  const url = request.url;
  const host = request.headers.get('host') || '';
  const hostname = host.split(':')[0];

  // Local development environment
  if (url.includes('localhost') || url.includes('127.0.0.1')) {
    // Try to extract subdomain from the full URL
    const fullUrlMatch = url.match(/http:\/\/([^.]+)\.localhost/);
    if (fullUrlMatch && fullUrlMatch[1]) {
      return fullUrlMatch[1];
    }

    // Fallback to host header approach
    if (hostname.includes('.localhost')) {
      return hostname.split('.')[0];
    }

    return null;
  }

  // Production environment
  const rootDomainFormatted = rootDomain.split(':')[0];

  // Handle preview deployment URLs (tenant---branch-name.vercel.app)
  if (hostname.includes('---') && hostname.endsWith('.vercel.app')) {
    const parts = hostname.split('---');
    return parts.length > 0 ? parts[0] : null;
  }

  // Regular subdomain detection
  const isSubdomain =
    hostname !== rootDomainFormatted &&
    hostname !== `www.${rootDomainFormatted}` &&
    hostname.endsWith(`.${rootDomainFormatted}`);

  return isSubdomain ? hostname.replace(`.${rootDomainFormatted}`, '') : null;
}

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const subdomain = extractSubdomain(request);

  if (subdomain) {
    // Block access to admin page from subdomains
    if (pathname.startsWith('/admin')) {
      return NextResponse.redirect(new URL('/', request.url));
    }

    // For the root path on a subdomain, rewrite to the subdomain page
    if (pathname === '/') {
      return NextResponse.rewrite(new URL(`/s/${subdomain}`, request.url));
    }
  }

  // On the root domain, allow normal access
  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all paths except for:
     * 1. /api routes
     * 2. /_next (Next.js internals)
     * 3. all root files inside /public (e.g. /favicon.ico)
     */
    '/((?!api|_next|[\\w-]+\\.\\w+).*)'
  ]
};
```

this is my step2 to complete the backend and frontend till this point. 
this is my plan, i need to move to step 2. can you help me do it step by step. please go step by step and let me complete a step then move to the next step.