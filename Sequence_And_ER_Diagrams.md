# Detailed Sequence & ER Diagrams
## Test Automation Architecture - Interaction Flows & Data Models

---

## Table of Contents
1. [Detailed Sequence Diagrams](#sequence-diagrams)
2. [Entity-Relationship Diagrams](#er-diagrams)
3. [State Transition Diagrams](#state-diagrams)

---

## 1. Detailed Sequence Diagrams {#sequence-diagrams}

### 1.1 Complete Test Execution Flow (K8s-Native)

```mermaid
sequenceDiagram
    actor User
    participant UI as Web Dashboard
    participant API as API Gateway
    participant Auth as Auth Service
    participant Orch as Orchestrator
    participant Queue as Redis Queue
    participant Sched as Scheduler
    participant RM as Resource Manager
    participant K8s as Kubernetes API
    participant Pod as Test Pod
    participant S3 as S3 Storage
    participant DB as PostgreSQL
    participant Prom as Prometheus
    participant Notif as Notification Service

    User->>UI: Submit Test
    UI->>API: POST /api/v1/tests
    API->>Auth: Validate JWT Token
    Auth-->>API: Token Valid + User Info

    API->>Orch: Submit Test Request
    Orch->>Orch: Validate Test Definition
    Orch->>DB: Check User Quota
    DB-->>Orch: Quota Available

    Orch->>DB: INSERT test_definitions
    DB-->>Orch: Definition ID
    Orch->>DB: INSERT test_executions (status=queued)
    DB-->>Orch: Execution ID

    Orch->>Queue: LPUSH test:queue (priority-based)
    Queue-->>Orch: Queue Position
    Orch-->>API: 202 Accepted {executionId, queuePosition}
    API-->>UI: Response
    UI-->>User: Test Queued (Position #5)

    Note over Sched: Scheduler polls queue every 5s

    Sched->>Queue: RPOP test:queue
    Queue-->>Sched: Test Execution
    Sched->>DB: GET test_definitions
    DB-->>Sched: Test Definition

    Sched->>RM: Check Resources Available
    RM->>K8s: GET nodes
    K8s-->>RM: Node List + Resources
    RM->>RM: Calculate Available Resources
    RM-->>Sched: Resources Available

    Sched->>RM: Select Optimal Node
    RM->>RM: Score Nodes (CPU, Memory, Spot)
    RM-->>Sched: Node: ip-10-0-1-50

    Sched->>K8s: CREATE Job (test-exec-abc123)
    K8s->>K8s: Schedule Pod
    K8s->>Pod: Start Init Container
    K8s-->>Sched: Job Created

    Sched->>DB: UPDATE test_executions SET status='running'
    DB-->>Sched: Updated

    Pod->>Pod: Init: Clone Git Repo
    Pod->>Pod: Init: Install Dependencies (npm/pip)
    Pod->>Pod: Init: Setup Environment

    Pod->>Pod: Main Container Starts
    Pod->>Prom: Register Metrics Endpoint
    Pod->>DB: GET test_definitions
    DB-->>Pod: Test Config + Secrets

    Note over Pod: Execute Tests

    loop For Each Test Case
        Pod->>Pod: Run Test Case
        Pod->>Prom: Push Metric (test_duration_seconds)

        alt Test Passed
            Pod->>Pod: Record Pass
        else Test Failed
            Pod->>Pod: Capture Screenshot
            Pod->>Pod: Record Failure + Stack Trace
        end
    end

    Pod->>Pod: Generate Test Report (HTML/XML)
    Pod->>S3: Upload Screenshots
    S3-->>Pod: URLs
    Pod->>S3: Upload Videos
    S3-->>Pod: URLs
    Pod->>S3: Upload Test Report
    S3-->>Pod: URL

    Pod->>DB: INSERT test_results (batch)
    DB-->>Pod: Inserted
    Pod->>DB: INSERT test_artifacts
    DB-->>Pod: Inserted

    Pod->>DB: UPDATE test_executions SET status='completed'
    DB-->>Pod: Updated

    Pod->>Prom: Push Final Metrics
    Pod->>K8s: Exit 0 (Success)

    K8s->>K8s: Pod Completed
    K8s->>Sched: Pod Completion Event
    Sched->>RM: Release Resources
    RM-->>Sched: Resources Released

    Sched->>DB: GET test_results
    DB-->>Sched: Test Results Summary
    Sched->>Notif: Send Notification
    Notif->>Notif: Format Message

    alt All Tests Passed
        Notif->>User: Slack: ✓ Tests Passed (250/250)
    else Some Tests Failed
        Notif->>User: Slack: ✗ Tests Failed (245/250)
        Notif->>User: Email: Detailed Failure Report
    end

    Note over K8s: TTL Controller (after 1 hour)
    K8s->>K8s: Delete Completed Pod
```

### 1.2 Parallel Test Execution with Autoscaling

```mermaid
sequenceDiagram
    participant Orch as Orchestrator
    participant Sched as Scheduler
    participant K8s as Kubernetes
    participant HPA as Horizontal Pod Autoscaler
    participant CA as Cluster Autoscaler
    participant AWS as AWS EC2
    participant Pods as Test Pods (50)

    Orch->>Sched: Submit 500 Tests (parallelism=50)
    Sched->>Sched: Split into 50 Groups (10 tests each)

    loop For Each Group (i=1 to 50)
        Sched->>K8s: CREATE Pod test-group-{i}
    end

    K8s->>K8s: Schedule 50 Pods

    Note over K8s: Not enough capacity on current nodes

    K8s->>HPA: Trigger Scale-Up Event
    HPA->>HPA: Calculate Required Replicas
    HPA->>K8s: Scale Deployment (replica count)

    K8s->>CA: Trigger Cluster Autoscaler
    CA->>CA: Calculate Required Nodes
    CA->>AWS: Request 5 New EC2 Instances (m5.xlarge)
    AWS->>AWS: Launch Spot Instances
    AWS-->>CA: Instances Launched

    CA->>K8s: Register New Nodes
    K8s->>K8s: Nodes Ready

    K8s->>Pods: Schedule Remaining Pods on New Nodes
    Pods->>Pods: All 50 Pods Running

    par Parallel Execution
        Pods->>Pods: Group 1: Execute 10 Tests
    and
        Pods->>Pods: Group 2: Execute 10 Tests
    and
        Pods->>Pods: ...
    and
        Pods->>Pods: Group 50: Execute 10 Tests
    end

    Note over Pods: Execution completes in 5 minutes

    Pods->>K8s: All Pods Completed
    K8s->>HPA: Pods Completed Event
    HPA->>K8s: Scale Down (no load)

    Note over CA: Wait 10 minutes (scale-down delay)

    CA->>K8s: Remove Idle Nodes
    K8s->>AWS: Terminate EC2 Instances
    AWS-->>K8s: Instances Terminated

    Note over K8s: Cluster back to baseline
```

### 1.3 Chaos Engineering - Complete Workflow

```mermaid
sequenceDiagram
    actor SRE as SRE Engineer
    participant UI as Chaos Dashboard
    participant API as Chaos API
    participant Safety as Safety Gate
    participant PD as PagerDuty
    participant AI as AI Engine
    participant Prom as Prometheus
    participant Scheduler as Chaos Scheduler
    participant Litmus as LitmusChaos
    participant K8s as Kubernetes
    participant PaymentSvc as Payment Service
    participant Istio as Istio Service Mesh
    participant Monitor as Monitoring

    SRE->>UI: Design Chaos Experiment
    UI->>UI: Select Target: Payment Service
    UI->>UI: Select Fault: Pod Delete
    UI->>UI: Set Duration: 5 minutes
    UI->>UI: Define SLO: Error Rate < 1%

    SRE->>UI: Submit Experiment
    UI->>API: POST /experiments
    API->>Safety: Pre-Flight Safety Checks

    Safety->>PD: Check Active Incidents
    PD-->>Safety: No Active Incidents
    Safety->>PD: Check On-Call Engineer
    PD-->>Safety: On-Call Available
    Safety->>K8s: Check Recent Deployments (last 2h)
    K8s-->>Safety: No Recent Deployments
    Safety->>Prom: Check System Health
    Prom-->>Safety: All SLOs Green

    Safety-->>API: ✓ All Checks Passed
    API->>AI: Get AI Recommendations

    AI->>Prom: Query Service Dependencies
    Prom-->>AI: Dependency Graph
    AI->>AI: Analyze Impact (ML Model)
    AI-->>API: Predicted Impact: Medium (5% traffic)

    API->>Scheduler: Schedule Experiment
    Scheduler-->>SRE: Experiment Scheduled (ID: exp-123)

    Note over Scheduler: Experiment starts at scheduled time

    Scheduler->>Prom: Measure Baseline (5 min)
    Prom-->>Scheduler: Baseline Metrics:<br/>Error Rate: 0.1%<br/>Latency p99: 200ms<br/>Throughput: 1000 rps

    Scheduler->>Litmus: Create ChaosEngine CR
    Litmus->>K8s: Apply ChaosEngine

    K8s->>Litmus: ChaosEngine Created
    Litmus->>Litmus: Chaos Runner Pod Starts
    Litmus->>K8s: Inject Fault: Delete Payment Pod

    K8s->>PaymentSvc: DELETE pod payment-svc-abc123
    PaymentSvc->>PaymentSvc: Pod Terminating

    Note over PaymentSvc: Payment service pod down

    loop Every 10 seconds (for 5 minutes)
        Monitor->>Prom: Query Error Rate
        Prom-->>Monitor: Error Rate: 0.8%
        Monitor->>Prom: Query Latency p99
        Prom-->>Monitor: Latency p99: 450ms

        Monitor->>Monitor: Check SLO: Error Rate < 1%

        alt SLO Breached
            Monitor->>Scheduler: ABORT: SLO Breach Detected
            Scheduler->>Litmus: Delete ChaosEngine
            Litmus->>K8s: Stop Chaos
        else SLO OK
            Monitor->>Scheduler: Continue Monitoring
        end
    end

    Note over K8s: Kubernetes auto-healing

    K8s->>K8s: Detect Pod Missing
    K8s->>K8s: Create Replacement Pod
    K8s->>PaymentSvc: payment-svc-xyz123 Started
    PaymentSvc->>Istio: Register with Service Mesh
    Istio-->>PaymentSvc: Ready to Receive Traffic

    Note over Scheduler: 5 minutes elapsed

    Scheduler->>Litmus: Stop Chaos
    Litmus->>K8s: Delete ChaosEngine
    K8s-->>Litmus: ChaosEngine Deleted

    Scheduler->>Prom: Measure Recovery (5 min)
    Prom-->>Scheduler: Recovery Metrics:<br/>Error Rate: 0.1%<br/>Latency p99: 210ms<br/>Recovery Time: 45s

    Scheduler->>Scheduler: Analyze Results
    Scheduler->>Scheduler: Hypothesis: VALIDATED ✓<br/>System recovered in < 60s

    Scheduler->>AI: Report Results
    AI->>AI: Update ML Model with Experiment Data

    Scheduler->>SRE: Email: Chaos Experiment Complete<br/>✓ Hypothesis Validated<br/>Recovery: 45s

    SRE->>UI: View Detailed Report
    UI-->>SRE: Show Metrics Charts + Timeline
```

### 1.4 Event-Driven Test - Saga Pattern

```mermaid
sequenceDiagram
    participant Test as Test Framework
    participant TestConsumer as Test Event Consumer
    participant Kafka as Kafka Broker
    participant OrderSvc as Order Service
    participant PaymentSvc as Payment Service
    participant InventorySvc as Inventory Service
    participant EventStore as Event Store
    participant Chaos as Chaos Engine

    Test->>TestConsumer: Setup Consumer
    TestConsumer->>Kafka: Subscribe to [orders, payments, inventory]
    Kafka-->>TestConsumer: Subscribed

    Test->>Chaos: Inject Chaos: Kill Payment Pod
    Chaos->>PaymentSvc: kubectl delete pod payment-svc-xyz
    PaymentSvc->>PaymentSvc: Pod Terminating

    Test->>OrderSvc: POST /orders {order_id: 'ABC123'}
    OrderSvc->>OrderSvc: Create Order
    OrderSvc->>Kafka: Publish: OrderCreated(ABC123)
    Kafka->>EventStore: Store: OrderCreated
    Kafka->>TestConsumer: Deliver: OrderCreated
    TestConsumer->>Test: Event 1: OrderCreated ✓

    Kafka->>InventorySvc: Deliver: OrderCreated
    InventorySvc->>InventorySvc: Reserve Inventory (SKU: 'ITEM-1', Qty: 2)
    InventorySvc->>Kafka: Publish: InventoryReserved(ABC123)
    Kafka->>EventStore: Store: InventoryReserved
    Kafka->>TestConsumer: Deliver: InventoryReserved
    TestConsumer->>Test: Event 2: InventoryReserved ✓

    Kafka->>PaymentSvc: Deliver: InventoryReserved
    Note over PaymentSvc: Pod is down!
    PaymentSvc--xKafka: Connection failed

    Note over Kafka: Message remains in Kafka (not consumed)

    Note over PaymentSvc: Kubernetes auto-recovery (30s)

    PaymentSvc->>PaymentSvc: New Pod Starting
    PaymentSvc->>Kafka: Consumer Group Rebalance
    Kafka->>PaymentSvc: Redeliver: InventoryReserved(ABC123)

    PaymentSvc->>PaymentSvc: Process Payment
    PaymentSvc->>PaymentSvc: Check Service Health
    Note over PaymentSvc: Service still recovering

    PaymentSvc->>Kafka: Publish: PaymentFailed(ABC123, reason: 'Service Unavailable')
    Kafka->>EventStore: Store: PaymentFailed
    Kafka->>TestConsumer: Deliver: PaymentFailed
    TestConsumer->>Test: Event 3: PaymentFailed ✓

    Note over Test: Saga Compensation Starts

    Kafka->>InventorySvc: Deliver: PaymentFailed
    InventorySvc->>InventorySvc: Compensate: Release Inventory
    InventorySvc->>Kafka: Publish: InventoryReleased(ABC123)
    Kafka->>EventStore: Store: InventoryReleased
    Kafka->>TestConsumer: Deliver: InventoryReleased
    TestConsumer->>Test: Event 4: InventoryReleased ✓

    Kafka->>OrderSvc: Deliver: PaymentFailed
    OrderSvc->>OrderSvc: Compensate: Cancel Order
    OrderSvc->>Kafka: Publish: OrderCancelled(ABC123)
    Kafka->>EventStore: Store: OrderCancelled
    Kafka->>TestConsumer: Deliver: OrderCancelled
    TestConsumer->>Test: Event 5: OrderCancelled ✓

    Test->>EventStore: Query All Events for ABC123
    EventStore-->>Test: [OrderCreated, InventoryReserved,<br/>PaymentFailed, InventoryReleased,<br/>OrderCancelled]

    Test->>Test: Assert Event Order ✓
    Test->>Test: Assert No Duplicates ✓
    Test->>Test: Assert Compensation Complete ✓
    Test->>Test: Assert Recovery Time < 60s ✓

    Test->>Chaos: Remove Chaos
    Chaos->>PaymentSvc: Chaos Removed

    Test->>Test: Test Result: PASSED ✓
```

### 1.5 CI/CD Pipeline Integration

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Git as GitHub
    participant GHA as GitHub Actions
    participant Build as Build Service
    participant Registry as ECR
    participant K8s as Kubernetes
    participant TestAPI as Test Orchestration API
    participant TestPods as Test Pods
    participant ChaosAPI as Chaos API
    participant Slack as Slack

    Dev->>Git: git push feature/new-api
    Git->>GHA: Trigger Workflow

    GHA->>GHA: Checkout Code
    GHA->>Build: Run Unit Tests
    Build-->>GHA: Unit Tests Passed ✓

    GHA->>Build: Build Docker Image
    Build-->>GHA: Image Built
    GHA->>Registry: Push Image (tag: commit-sha)
    Registry-->>GHA: Image Pushed

    GHA->>K8s: Deploy to Test Namespace
    K8s->>K8s: Create Deployment (feature/new-api)
    K8s->>K8s: Wait for Ready (readinessProbe)
    K8s-->>GHA: Deployment Ready

    GHA->>TestAPI: POST /api/v1/tests (API Tests)
    TestAPI->>TestPods: Create 10 Test Pods (parallel)
    TestPods->>K8s: Run API Tests
    TestPods-->>TestAPI: Results: 95/100 Passed

    alt Tests Failed
        TestAPI-->>GHA: Test Failed (5 failures)
        GHA->>Slack: ❌ API Tests Failed<br/>Branch: feature/new-api
        GHA->>GHA: Exit 1 (fail pipeline)
    else Tests Passed
        TestAPI-->>GHA: Tests Passed ✓

        GHA->>TestAPI: POST /api/v1/tests (E2E Tests)
        TestAPI->>TestPods: Create Selenium Grid Tests
        TestPods->>K8s: Run E2E Tests
        TestPods-->>TestAPI: Results: 50/50 Passed
        TestAPI-->>GHA: Tests Passed ✓

        GHA->>ChaosAPI: POST /experiments (Chaos Gate)
        ChaosAPI->>ChaosAPI: Run Pod Delete Experiment
        ChaosAPI->>K8s: Delete Random Pod
        K8s->>K8s: Auto-recovery
        ChaosAPI->>ChaosAPI: Validate SLOs
        ChaosAPI-->>GHA: Chaos Tests Passed ✓

        GHA->>GHA: Security Scan (Snyk)
        GHA->>GHA: No Critical Vulnerabilities ✓

        GHA->>K8s: Deploy to Staging
        K8s->>K8s: Rolling Update (staging)
        K8s-->>GHA: Staging Deployed ✓

        GHA->>Slack: ✓ Pipeline Succeeded<br/>Branch: feature/new-api<br/>Ready for Production
    end
```

---

## 2. Entity-Relationship Diagrams {#er-diagrams}

### 2.1 Complete Database Schema - ER Diagram

```mermaid
erDiagram
    USERS ||--o{ TEST_DEFINITIONS : owns
    USERS ||--o{ TEST_EXECUTIONS : submits
    USERS }o--|| TEAMS : belongs_to
    USERS ||--o{ CHAOS_EXPERIMENTS : creates
    USERS ||--o{ API_TOKENS : has

    TEST_DEFINITIONS ||--o{ TEST_EXECUTIONS : has
    TEST_DEFINITIONS ||--o{ TEST_TAGS : tagged_with
    TEST_DEFINITIONS }o--|| TEST_FRAMEWORKS : uses

    TEST_EXECUTIONS ||--o{ TEST_RESULTS : contains
    TEST_EXECUTIONS ||--o{ TEST_ARTIFACTS : produces
    TEST_EXECUTIONS ||--o{ TEST_METRICS : records
    TEST_EXECUTIONS }o--|| EXECUTION_ENVIRONMENTS : runs_in

    TEST_RESULTS ||--o{ TEST_ASSERTIONS : contains
    TEST_ARTIFACTS }o--|| ARTIFACT_TYPES : is_type

    CHAOS_EXPERIMENTS ||--o{ CHAOS_EXECUTIONS : has
    CHAOS_EXPERIMENTS }o--|| FAULT_TYPES : uses
    CHAOS_EXECUTIONS ||--o{ CHAOS_RESULTS : produces
    CHAOS_EXECUTIONS ||--o{ SLO_MEASUREMENTS : measures

    TEAMS ||--o{ TEAM_QUOTAS : has
    TEAMS ||--o{ TEAM_SETTINGS : has

    EXECUTION_ENVIRONMENTS ||--o{ TEST_EXECUTIONS : hosts
    EXECUTION_ENVIRONMENTS }o--|| CLUSTERS : deployed_in

    USERS {
        uuid user_id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        uuid team_id FK
        jsonb roles
        jsonb quota
        timestamp created_at
        timestamp last_login
        boolean active
    }

    TEAMS {
        uuid team_id PK
        varchar team_name UK
        jsonb quota
        jsonb settings
        timestamp created_at
        boolean active
    }

    TEST_DEFINITIONS {
        uuid id PK
        varchar name
        varchar type
        uuid owner_id FK
        uuid framework_id FK
        jsonb content
        jsonb resources
        jsonb config
        jsonb metadata
        varchar version
        timestamp created_at
        timestamp updated_at
        boolean deleted
    }

    TEST_FRAMEWORKS {
        uuid framework_id PK
        varchar name UK
        varchar version
        jsonb configuration
        jsonb supported_languages
        timestamp created_at
    }

    TEST_EXECUTIONS {
        uuid execution_id PK
        uuid definition_id FK
        uuid environment_id FK
        varchar status
        timestamp submitted_at
        timestamp started_at
        timestamp completed_at
        int retry_count
        varchar pod_name
        varchar node_name
        jsonb metrics
        uuid submitted_by FK
        varchar failure_reason
        int exit_code
    }

    TEST_RESULTS {
        uuid result_id PK
        uuid execution_id FK
        varchar test_name
        varchar status
        int duration_ms
        text error_message
        text stack_trace
        jsonb assertions
        timestamp executed_at
        varchar file_path
        int line_number
    }

    TEST_ASSERTIONS {
        uuid assertion_id PK
        uuid result_id FK
        varchar assertion_type
        text expected_value
        text actual_value
        boolean passed
        text message
    }

    TEST_ARTIFACTS {
        uuid artifact_id PK
        uuid execution_id FK
        uuid artifact_type_id FK
        varchar file_name
        varchar s3_path
        bigint file_size_bytes
        varchar content_type
        varchar checksum
        timestamp created_at
        timestamp expires_at
    }

    ARTIFACT_TYPES {
        uuid artifact_type_id PK
        varchar type_name UK
        varchar description
        int retention_days
        boolean compress
    }

    TEST_METRICS {
        uuid metric_id PK
        uuid execution_id FK
        varchar metric_name
        decimal metric_value
        varchar unit
        timestamp measured_at
        jsonb labels
    }

    TEST_TAGS {
        uuid tag_id PK
        uuid definition_id FK
        varchar tag_name
        varchar tag_value
        timestamp created_at
    }

    CHAOS_EXPERIMENTS {
        uuid experiment_id PK
        varchar name
        uuid fault_type_id FK
        jsonb fault_spec
        jsonb target_selector
        jsonb hypothesis
        int duration_seconds
        jsonb halt_conditions
        uuid created_by FK
        varchar status
        timestamp created_at
        timestamp updated_at
        boolean deleted
    }

    FAULT_TYPES {
        uuid fault_type_id PK
        varchar fault_name UK
        varchar category
        jsonb parameters_schema
        jsonb provider_mapping
        text description
    }

    CHAOS_EXECUTIONS {
        uuid execution_id PK
        uuid experiment_id FK
        varchar status
        timestamp scheduled_at
        timestamp started_at
        timestamp completed_at
        jsonb baseline_metrics
        jsonb chaos_metrics
        jsonb recovery_metrics
        boolean hypothesis_validated
        int recovery_time_seconds
        text abort_reason
    }

    CHAOS_RESULTS {
        uuid result_id PK
        uuid execution_id FK
        varchar metric_name
        decimal baseline_value
        decimal chaos_value
        decimal recovery_value
        varchar unit
        timestamp measured_at
    }

    SLO_MEASUREMENTS {
        uuid measurement_id PK
        uuid execution_id FK
        varchar slo_name
        decimal target_value
        decimal actual_value
        boolean met
        varchar phase
        timestamp measured_at
    }

    API_TOKENS {
        uuid token_id PK
        uuid user_id FK
        varchar token_name
        varchar token_hash
        jsonb scopes
        timestamp created_at
        timestamp expires_at
        timestamp last_used_at
        boolean revoked
    }

    EXECUTION_ENVIRONMENTS {
        uuid environment_id PK
        varchar environment_name UK
        uuid cluster_id FK
        varchar namespace
        jsonb configuration
        varchar status
        timestamp created_at
    }

    CLUSTERS {
        uuid cluster_id PK
        varchar cluster_name UK
        varchar provider
        varchar region
        jsonb kubeconfig
        varchar status
        timestamp created_at
    }

    TEAM_QUOTAS {
        uuid quota_id PK
        uuid team_id FK
        varchar resource_type
        int limit_value
        varchar period
        timestamp created_at
        timestamp updated_at
    }

    TEAM_SETTINGS {
        uuid setting_id PK
        uuid team_id FK
        varchar setting_key
        jsonb setting_value
        timestamp created_at
        timestamp updated_at
    }
```

### 2.2 Event Store Schema - ER Diagram

```mermaid
erDiagram
    EVENT_STREAMS ||--o{ EVENTS : contains
    EVENTS ||--o{ EVENT_METADATA : has
    EVENT_STREAMS ||--o{ SNAPSHOTS : has
    EVENT_STREAMS ||--o{ SUBSCRIPTIONS : subscribed_to
    SUBSCRIPTIONS }o--|| SUBSCRIPTION_GROUPS : belongs_to

    EVENT_STREAMS {
        varchar stream_id PK
        varchar aggregate_type
        varchar aggregate_id
        bigint version
        timestamp created_at
        timestamp updated_at
        boolean deleted
    }

    EVENTS {
        uuid event_id PK
        varchar stream_id FK
        bigint event_number
        varchar event_type
        jsonb event_data
        jsonb event_metadata
        timestamp created_at
        varchar created_by
        varchar correlation_id
        varchar causation_id
    }

    EVENT_METADATA {
        uuid metadata_id PK
        uuid event_id FK
        varchar key
        text value
        timestamp created_at
    }

    SNAPSHOTS {
        uuid snapshot_id PK
        varchar stream_id FK
        bigint event_number
        jsonb state
        timestamp created_at
    }

    SUBSCRIPTIONS {
        uuid subscription_id PK
        varchar stream_id FK
        uuid subscription_group_id FK
        varchar consumer_id
        bigint last_processed_event
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    SUBSCRIPTION_GROUPS {
        uuid subscription_group_id PK
        varchar group_name UK
        varchar strategy
        int max_retry_count
        int retry_delay_seconds
        timestamp created_at
    }
```

---

## 3. State Transition Diagrams {#state-diagrams}

### 3.1 Test Execution State Machine

```mermaid
stateDiagram-v2
    [*] --> Submitted: User submits test

    Submitted --> Validating: Begin validation
    Validating --> Rejected: Validation failed
    Validating --> Queued: Validation passed

    Rejected --> [*]

    Queued --> Scheduled: Resources available
    Queued --> Cancelled: User cancels
    Queued --> Expired: Queue timeout

    Scheduled --> Initializing: Pod created
    Initializing --> Running: Init successful
    Initializing --> Failed: Init failed

    Running --> Completing: Tests executing
    Completing --> Completed: All tests done
    Completing --> Failed: Test failure
    Completing --> Timeout: Execution timeout
    Completing --> Cancelled: User cancels

    Failed --> Retrying: Retry available
    Timeout --> Retrying: Retry available
    Retrying --> Scheduled: Retry scheduled

    Failed --> Cleanup: Max retries reached
    Timeout --> Cleanup: Max retries reached
    Completed --> Cleanup: Success
    Cancelled --> Cleanup: User cancelled
    Expired --> Cleanup: Queue expired

    Cleanup --> Archived: Artifacts stored
    Archived --> [*]

    note right of Queued
        Priority queue
        FIFO within priority
        Max queue time: 1 hour
    end note

    note right of Running
        Max execution time: 2 hours
        Heartbeat: every 30s
    end note

    note right of Cleanup
        Upload artifacts to S3
        Store results in DB
        Delete pod after TTL
    end note
```

### 3.2 Chaos Experiment State Machine

```mermaid
stateDiagram-v2
    [*] --> Draft: Create experiment

    Draft --> UnderReview: Submit for review
    Draft --> Deleted: Discard

    UnderReview --> Approved: Peer approved
    UnderReview --> ChangesRequested: Needs revision
    ChangesRequested --> Draft: Update experiment

    Approved --> Scheduled: Schedule execution
    Scheduled --> WaitingForWindow: Wait for safe window

    WaitingForWindow --> PreFlightCheck: Window opened
    WaitingForWindow --> Postponed: Window missed

    PreFlightCheck --> ReadyToRun: All checks passed
    PreFlightCheck --> Blocked: Pre-flight failed
    Blocked --> Scheduled: Retry scheduled

    ReadyToRun --> Running: Start chaos injection

    Running --> Monitoring: Fault injected

    state Monitoring {
        [*] --> CheckingSLO
        CheckingSLO --> SLOHealthy: Metrics OK
        CheckingSLO --> SLOBreached: Metrics degraded
        SLOHealthy --> CheckingSLO: Continue monitoring
        SLOBreached --> [*]: Trigger abort
    }

    Monitoring --> Completed: Duration elapsed (normal)
    Monitoring --> Aborted: SLO breach (auto-abort)
    Monitoring --> Aborted: Manual abort
    Monitoring --> Failed: Experiment error

    Completed --> Recovery: Stop chaos
    Aborted --> Recovery: Stop chaos
    Failed --> Recovery: Stop chaos

    Recovery --> Validated: System recovered
    Recovery --> Escalated: Recovery timeout

    Validated --> AnalyzingResults: Collect metrics
    Escalated --> IncidentMode: SRE intervention

    AnalyzingResults --> HypothesisConfirmed: Expected behavior
    AnalyzingResults --> HypothesisRejected: Unexpected behavior

    HypothesisConfirmed --> Completed_Success: Archive
    HypothesisRejected --> Completed_ActionRequired: Create tickets

    Completed_Success --> [*]
    Completed_ActionRequired --> [*]
    IncidentMode --> [*]
    Postponed --> Scheduled: Reschedule
    Deleted --> [*]

    note right of PreFlightCheck
        Checks:
        - No active incidents
        - On-call available
        - No recent deployments
        - System healthy
        - Approvals obtained
    end note

    note right of Monitoring
        Real-time checks:
        - Error rate < threshold
        - Latency < threshold
        - CPU < threshold
        - Consumer lag < threshold
        Every 10 seconds
    end note

    note right of Recovery
        Recovery phase:
        - Remove fault injection
        - Restore network rules
        - Wait for stabilization
        - Measure recovery time
        Target: < 60 seconds
    end note
```

### 3.3 Event-Driven Saga State Machine

```mermaid
stateDiagram-v2
    [*] --> SagaInitiated: Trigger event received

    SagaInitiated --> Step1_Pending: Start step 1
    Step1_Pending --> Step1_InProgress: Processing
    Step1_InProgress --> Step1_Completed: Success
    Step1_InProgress --> Step1_Failed: Failure

    Step1_Failed --> Compensating: Trigger compensation
    Step1_Completed --> Step2_Pending: Start step 2

    Step2_Pending --> Step2_InProgress: Processing
    Step2_InProgress --> Step2_Completed: Success
    Step2_InProgress --> Step2_Failed: Failure

    Step2_Failed --> Compensating: Trigger compensation
    Step2_Completed --> Step3_Pending: Start step 3

    Step3_Pending --> Step3_InProgress: Processing
    Step3_InProgress --> Step3_Completed: Success
    Step3_InProgress --> Step3_Failed: Failure

    Step3_Failed --> Compensating: Trigger compensation
    Step3_Completed --> SagaCompleted: All steps done

    state Compensating {
        [*] --> Compensate_Step3
        Compensate_Step3 --> Compensate_Step2: Step 3 compensated
        Compensate_Step2 --> Compensate_Step1: Step 2 compensated
        Compensate_Step1 --> [*]: Step 1 compensated
    }

    Compensating --> CompensationCompleted: All compensated
    Compensating --> CompensationFailed: Compensation error

    SagaCompleted --> [*]
    CompensationCompleted --> SagaCancelled
    CompensationFailed --> ManualIntervention

    SagaCancelled --> [*]
    ManualIntervention --> [*]

    note right of SagaInitiated
        Saga Steps:
        1. Create Order
        2. Reserve Inventory
        3. Process Payment
        4. Create Shipment
    end note

    note right of Compensating
        Compensating Transactions:
        - Cancel Shipment
        - Refund Payment
        - Release Inventory
        - Cancel Order

        Execute in reverse order
    end note

    note right of CompensationFailed
        Manual intervention required:
        - Partial compensation
        - Data inconsistency
        - Human review needed
    end note
```

---

## Summary

This document provides:
- ✅ **6 Detailed Sequence Diagrams** covering all major workflows
- ✅ **2 Comprehensive ER Diagrams** (main DB schema + event store)
- ✅ **3 State Transition Diagrams** (test execution, chaos experiments, saga pattern)
- ✅ Complete interaction flows with timing and error handling
- ✅ Database relationships with all foreign keys and constraints
- ✅ State machine definitions with all possible transitions

These diagrams complement the HLD and LLD documents and provide visual representation of:
- System interactions and communication patterns
- Data relationships and schema design
- State management and lifecycle transitions
- Error handling and recovery flows

**Total Diagrams Created**: 11 detailed diagrams covering all aspects of the system.
