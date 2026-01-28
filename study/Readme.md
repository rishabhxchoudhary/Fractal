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
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fractal.backend.config.SecurityConfig;
import com.fractal.backend.controller.HealthCheckController;

@WebMvcTest(HealthCheckController.class) // Focus test on this controller
@Import(SecurityConfig.class) // Import our new security config
public class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToProtectedEndpointWithoutAuth() throws Exception {
        // When we access a protected endpoint without being logged in,
        // Spring Security should redirect us to the login page.
        // The HTTP status for a redirect is 302 (Found).
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isFound());
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

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.model.User;
import com.fractal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor // Lombok: Creates constructor for final fields (Injection)
public class AuthService {

    private final UserRepository userRepository;
    
    // We will inject WorkspaceRepository later when we build that part

    @Transactional
    public LoginResponse loginOrSignup(String email, String fullName, String avatarUrl) {
        // 1. Try to find user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 2. If not found, create new
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setAvatarUrl(avatarUrl);
                    return userRepository.save(newUser);
                });

        // 3. TODO: Fetch workspaces for this user (Empty for now)
        
        return LoginResponse.builder()
                .user(user)
                .workspaces(new ArrayList<>()) 
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Authorize Requests
                .authorizeHttpRequests(auth -> auth
                        // Allow unauthenticated access to the health check endpoint
                        .requestMatchers("/api/health").permitAll()
                        // All other requests under /api/ must be authenticated
                        .requestMatchers("/api/**").authenticated()
                        // Any other request (like serving the frontend) can be permitted
                        .anyRequest().permitAll())
                // 2. Configure OAuth2 Login
                .oauth2Login(withDefaults()); // This enables the default Google login flow

        return http.build();
    }
}
```

- in backend/src/main/java/com/fractal/backend/security/CustomOAuth2AuthenticationSuccessHandler.java
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

import com.fractal.backend.dto.LoginResponse;
import com.fractal.backend.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j // Lombok annotation for logging
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    // We will get this value from our application.properties or .env file
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1. Cast the authentication object to get user details
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();

        // 2. Extract user attributes provided by Google
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");

        // 3. Use our existing AuthService to find or create the user
        log.info("User {} successfully authenticated. Processing login/signup.", email);
        LoginResponse loginResponse = authService.loginOrSignup(email, name, avatarUrl);

        // 4. Determine where to redirect the user
        String redirectUrl;
        if (loginResponse.getWorkspaces().isEmpty()) {
            // If the user has no workspaces, send them to a "create workspace" page
            redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/welcome/new-workspace")
                .toUriString();
            log.info("New user or user with no workspaces. Redirecting to create workspace page.");
        } else {
            // If the user has workspaces, send them to the dashboard or a selector page
            // For now, let's just redirect to a generic dashboard path
            redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/dashboard")
                .toUriString();
             log.info("Existing user with workspaces. Redirecting to dashboard.");
        }
        
        // TODO: Later we will add a JWT token to the redirect URL for session management

        // 5. Perform the redirect
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CustomOAuth2AuthenticationSuccessHandler successHandler;

    private OAuth2User oAuth2User;
    private final String frontendUrl = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        // Manually set the frontendUrl field since @Value won't work in a plain Mockito test
        ReflectionTestUtils.setField(successHandler, "frontendUrl", frontendUrl);

        Map<String, Object> attributes = Map.of(
            "email", "test.user@google.com",
            "name", "Test User",
            "picture", "http://example.com/avatar.jpg"
        );
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

        // --- Act ---
        successHandler.onAuthenticationSuccess(request, response, token);

        // --- Assert ---
        verify(authService).loginOrSignup(
            "test.user@google.com", 
            "Test User", 
            "http://example.com/avatar.jpg"
        );

        // Verify that the redirect goes to the "new workspace" page
        verify(response).sendRedirect(frontendUrl + "/welcome/new-workspace"); 
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
                .build()
        );
        LoginResponse loginResponse = LoginResponse.builder()
                .user(user)
                .workspaces(workspaces)
                .build();
        
        when(authService.loginOrSignup(anyString(), anyString(), anyString())).thenReturn(loginResponse);

        // --- Act ---
        successHandler.onAuthenticationSuccess(request, response, token);

        // --- Assert ---
        verify(authService).loginOrSignup(
            "test.user@google.com", 
            "Test User", 
            "http://example.com/avatar.jpg"
        );

        // Verify that the redirect goes to the dashboard
        verify(response).sendRedirect(frontendUrl + "/dashboard"); 
    }
}
```

created a project in google developer console called 'fractal',configured oauth concent screen, went to APIs & Services > Credentials and created oauth2 client added origin: http://localhost:8080 and redirect URI: http://localhost:8080/login/oauth2/code/google, added the credentials to .env in backend and frontend folder. 

Till this point everything is done and implemented by me. i want your help after this point. 


2. Create a home page and login I want to setup google oauth 2 for login. setup the backend in spring boot using TDD. the signup flow should be like notion/slack, where user must first create a workspace if he has not done yet, if he has multiple workspaces, he should select which one to go into. not sure if its easier to use https://authjs.dev/getting-started/migrating-to-v5 ? as i want to allow preview deployements to also be able to log in when i host this on vercel. 


this is my plan, i need to move to step 2. can you help me do it step by step. please go step by step and let me complete a step then move to the next step. I have never actually worked with JAVA before.  I also want to use .env file instead of hardcoding the credentials if possible as i am pushing these to github.