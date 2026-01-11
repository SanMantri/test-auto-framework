# Test Automation Architecture Diagrams
## Visual Guide to Enterprise Testing Systems

---

## Table of Contents
1. [Kubernetes-Native Distributed Test Orchestration Diagrams](#k8s-native)
2. [Event-Driven + Chaos Engineering Diagrams](#event-driven-chaos)
3. [Hybrid Architecture Diagrams](#hybrid)

---

## Architecture #1: Kubernetes-Native Distributed Test Orchestration {#k8s-native}

### 1.1 Class Diagram - Test Orchestration Platform

```mermaid
classDiagram
    class TestOrchestrator {
        -KubernetesClient k8sClient
        -TestScheduler scheduler
        -ResultAggregator aggregator
        -ResourceManager resourceMgr
        +submitTest(TestDefinition) TestExecution
        +cancelTest(string testId) void
        +getTestStatus(string testId) TestStatus
        +listTests(TestFilter) List~TestExecution~
    }

    class TestDefinition {
        -string id
        -string name
        -TestType type
        -ResourceRequirements resources
        -Map~string,string~ labels
        -TestContent content
        -ExecutionConfig config
        +validate() boolean
        +toKubernetesJob() Job
        +toKubernetesPod() Pod
    }

    class TestScheduler {
        -PriorityQueue~TestExecution~ queue
        -NodeSelector nodeSelector
        -ResourceQuota quota
        +schedule(TestExecution) void
        +getNextTest() TestExecution
        +prioritize(List~TestExecution~) void
        -checkResourceAvailability() boolean
        -selectOptimalNode() Node
    }

    class TestExecution {
        -string executionId
        -TestDefinition definition
        -ExecutionStatus status
        -DateTime startTime
        -DateTime endTime
        -int retryCount
        -Pod pod
        -Job job
        +start() void
        +stop() void
        +retry() void
        +getStatus() ExecutionStatus
        +getLogs() string
        +getMetrics() ExecutionMetrics
    }

    class ResourceManager {
        -HorizontalPodAutoscaler hpa
        -VerticalPodAutoscaler vpa
        -ResourceQuota quota
        -LimitRange limits
        +allocateResources(TestExecution) boolean
        +scaleUp(int desiredPods) void
        +scaleDown(int desiredPods) void
        +getAvailableResources() Resources
        +enforceQuota() void
    }

    class ResultAggregator {
        -ResultStore store
        -MetricsCollector metricsCollector
        -NotificationService notifier
        +collectResult(TestExecution) void
        +aggregateResults(string testSuiteId) TestReport
        +publishMetrics(TestMetrics) void
        +sendNotification(TestResult) void
    }

    class KubernetesClient {
        -ApiClient apiClient
        -CoreV1Api coreApi
        -BatchV1Api batchApi
        -CustomObjectsApi customApi
        +createPod(Pod) Pod
        +createJob(Job) Job
        +deletePod(string name) void
        +watchPodStatus(string name) Watch
        +getCustomResource(string name) Object
    }

    class TestContent {
        <<interface>>
        +getType() TestType
        +getExecutable() string
        +getArguments() List~string~
    }

    class GitTestContent {
        -string repository
        -string branch
        -string path
        -Credentials credentials
        +clone() void
        +getType() TestType
        +getExecutable() string
    }

    class InlineTestContent {
        -string script
        -string language
        +getType() TestType
        +getExecutable() string
    }

    class ArtifactTestContent {
        -string artifactUrl
        -string version
        +download() void
        +getType() TestType
        +getExecutable() string
    }

    class ExecutionConfig {
        -int parallelism
        -int timeout
        -RetryPolicy retryPolicy
        -Map~string,string~ env
        -List~VolumeMount~ volumes
        -ServiceAccount serviceAccount
    }

    class TestReport {
        -string testSuiteId
        -int totalTests
        -int passed
        -int failed
        -int skipped
        -Duration totalDuration
        -List~TestResult~ results
        +generateHtml() string
        +generateJson() string
        +calculatePassRate() double
    }

    TestOrchestrator --> TestScheduler
    TestOrchestrator --> ResultAggregator
    TestOrchestrator --> ResourceManager
    TestOrchestrator --> KubernetesClient
    TestScheduler --> TestExecution
    TestExecution --> TestDefinition
    TestDefinition --> TestContent
    TestDefinition --> ExecutionConfig
    TestContent <|-- GitTestContent
    TestContent <|-- InlineTestContent
    TestContent <|-- ArtifactTestContent
    ResultAggregator --> TestReport
```

### 1.2 Data Flow Diagram - Test Execution Pipeline

```mermaid
flowchart TD
    Start([User/CI System]) --> |Submit Test| API[Test API Gateway]
    API --> Validate{Validate Test<br/>Definition}
    Validate -->|Invalid| Error1[Return Error]
    Validate -->|Valid| Queue[Test Queue<br/>Redis/RabbitMQ]

    Queue --> Scheduler[Test Scheduler]
    Scheduler --> CheckRes{Check Resource<br/>Availability}
    CheckRes -->|Insufficient| Wait[Wait in Queue]
    Wait --> Scheduler
    CheckRes -->|Available| CreateJob[Create Kubernetes Job/Pod]

    CreateJob --> InitContainer[Init Container]
    InitContainer --> FetchTest[Fetch Test Code<br/>from Git/Artifact]
    FetchTest --> SetupEnv[Setup Test Environment<br/>Install Dependencies]
    SetupEnv --> MainContainer[Main Test Container]

    MainContainer --> RunTest[Execute Test]
    RunTest --> CollectLogs[Collect Logs<br/>Stdout/Stderr]
    CollectLogs --> CollectMetrics[Collect Metrics<br/>CPU/Memory/Duration]
    CollectMetrics --> CollectArtifacts[Collect Artifacts<br/>Screenshots/Videos/Reports]

    CollectArtifacts --> StoreResults[(Result Storage<br/>S3/MinIO)]
    StoreResults --> Aggregator[Result Aggregator]

    Aggregator --> UpdateDB[(Test Result DB<br/>PostgreSQL)]
    Aggregator --> PublishMetrics[Publish Metrics<br/>Prometheus]
    Aggregator --> SendEvent[Send Event<br/>Kafka/Webhook]

    PublishMetrics --> Dashboard[Grafana Dashboard]
    SendEvent --> Notification[Notification Service<br/>Slack/Email]
    UpdateDB --> ReportGen[Generate Test Report]

    RunTest --> |Success| Cleanup[Cleanup Pod/Job]
    RunTest --> |Failure| CheckRetry{Retry<br/>Available?}
    CheckRetry -->|Yes| Scheduler
    CheckRetry -->|No| Cleanup

    Cleanup --> End([Test Complete])

    subgraph Kubernetes Cluster
        CreateJob
        InitContainer
        FetchTest
        SetupEnv
        MainContainer
        RunTest
        CollectLogs
        CollectMetrics
        CollectArtifacts
    end

    subgraph Observability Stack
        PublishMetrics
        Dashboard
        CollectLogs
    end

    subgraph Storage Layer
        StoreResults
        UpdateDB
    end

    style Start fill:#90EE90
    style End fill:#FFB6C1
    style RunTest fill:#87CEEB
    style Kubernetes Cluster fill:#E6F3FF
    style Observability Stack fill:#FFF8DC
    style Storage Layer fill:#F0E68C
```

### 1.3 Sequence Diagram - Complete Test Execution Flow

```mermaid
sequenceDiagram
    actor User
    participant API as Test API
    participant Queue as Message Queue
    participant Scheduler as Test Scheduler
    participant K8s as Kubernetes API
    participant Pod as Test Pod
    participant ServiceMesh as Istio/Linkerd
    participant AUT as Application Under Test
    participant Storage as Object Storage
    participant Metrics as Prometheus
    participant Notifier as Notification Service

    User->>API: POST /tests (TestDefinition)
    API->>API: Validate test definition
    API->>Queue: Enqueue test
    API-->>User: 202 Accepted {executionId}

    Scheduler->>Queue: Poll for tests
    Queue-->>Scheduler: TestExecution
    Scheduler->>K8s: Check resource availability
    K8s-->>Scheduler: Resources available

    Scheduler->>K8s: Create Job/Pod
    K8s->>Pod: Start init container
    Pod->>Pod: Clone git repo / Download artifact
    Pod->>Pod: Install dependencies

    K8s->>Pod: Start main container
    Pod->>Pod: Initialize test framework
    Pod->>Metrics: Register pod metrics

    loop For each test case
        Pod->>ServiceMesh: Request to AUT
        ServiceMesh->>ServiceMesh: Apply traffic policies
        ServiceMesh->>AUT: Forward request
        AUT-->>ServiceMesh: Response
        ServiceMesh-->>Pod: Response with telemetry
        Pod->>Pod: Assert test result
        Pod->>Metrics: Record test metrics
    end

    Pod->>Pod: Generate test report
    Pod->>Storage: Upload artifacts (screenshots, videos)
    Pod->>Metrics: Push final metrics
    Pod->>K8s: Update pod status (Completed)

    K8s->>Scheduler: Pod completed event
    Scheduler->>Storage: Fetch test results
    Scheduler->>Scheduler: Aggregate results
    Scheduler->>Notifier: Send notification
    Notifier->>User: Slack/Email notification

    User->>API: GET /tests/{executionId}
    API->>Storage: Fetch results
    API-->>User: Test report + artifacts

    K8s->>K8s: Cleanup completed pods (after TTL)
```

### 1.4 Component Diagram - Kubernetes Test Infrastructure

```mermaid
graph TB
    subgraph External Systems
        CI[CI/CD Pipeline<br/>Jenkins/GitLab/GitHub Actions]
        SCM[Source Control<br/>GitHub/GitLab]
        Users[QA Engineers<br/>Developers]
    end

    subgraph Ingress Layer
        ALB[Application Load Balancer]
        Ingress[Kubernetes Ingress<br/>nginx/Traefik]
    end

    subgraph Test Orchestration Namespace
        API[Test API Service<br/>REST + WebSocket]
        WebUI[Test Dashboard UI<br/>React/Vue]
        Scheduler[Test Scheduler<br/>Pod]
        Executor[Test Executor<br/>Deployment]
        ResultSvc[Result Service<br/>Aggregation]
    end

    subgraph Execution Namespace
        TestPods[Test Pods<br/>Dynamic Jobs]
        SeleniumHub[Selenium Grid Hub<br/>StatefulSet]
        ChromeNodes[Chrome Nodes<br/>Deployment]
        FirefoxNodes[Firefox Nodes<br/>Deployment]
    end

    subgraph Service Mesh
        Istio[Istio Control Plane]
        Envoy[Envoy Sidecars]
    end

    subgraph Message Layer
        Redis[Redis<br/>Queue + Cache]
        Kafka[Kafka<br/>Event Streaming]
    end

    subgraph Storage Layer
        PVC[Persistent Volume Claims<br/>Test Data]
        S3[Object Storage<br/>S3/MinIO<br/>Artifacts]
        PostgreSQL[(PostgreSQL<br/>Test Results)]
    end

    subgraph Observability
        Prometheus[Prometheus<br/>Metrics]
        Grafana[Grafana<br/>Dashboards]
        Jaeger[Jaeger<br/>Distributed Tracing]
        ELK[ELK Stack<br/>Logs]
    end

    subgraph Autoscaling
        HPA[Horizontal Pod<br/>Autoscaler]
        VPA[Vertical Pod<br/>Autoscaler]
        CA[Cluster Autoscaler]
    end

    Users --> ALB
    CI --> ALB
    ALB --> Ingress
    Ingress --> API
    Ingress --> WebUI

    API --> Scheduler
    API --> ResultSvc
    Scheduler --> Redis
    Scheduler --> Executor
    Executor --> TestPods
    Executor --> SeleniumHub
    SeleniumHub --> ChromeNodes
    SeleniumHub --> FirefoxNodes

    TestPods --> Envoy
    ChromeNodes --> Envoy
    FirefoxNodes --> Envoy
    Envoy --> Istio

    TestPods --> SCM
    TestPods --> S3
    ResultSvc --> S3
    ResultSvc --> PostgreSQL
    ResultSvc --> Kafka

    TestPods --> Prometheus
    TestPods --> Jaeger
    TestPods --> ELK
    API --> Prometheus
    Scheduler --> Prometheus

    HPA --> TestPods
    HPA --> ChromeNodes
    VPA --> Scheduler
    CA --> TestPods

    Prometheus --> Grafana

    style TestPods fill:#87CEEB
    style Users fill:#90EE90
    style CI fill:#90EE90
    style S3 fill:#FFD700
    style PostgreSQL fill:#FFD700
```

### 1.5 Deployment Diagram - Multi-Cluster Architecture

```mermaid
graph TB
    subgraph Region US-East
        subgraph EKS-Prod-Cluster
            ProdNS[Production Namespace<br/>Application Under Test]
            ProdTests[Production Test Namespace<br/>Smoke Tests Only]
            ProdMesh[Service Mesh<br/>mTLS Enabled]
        end

        subgraph EKS-Test-Cluster
            TestNS1[Test Namespace - Team A]
            TestNS2[Test Namespace - Team B]
            TestNS3[Test Namespace - Integration]
            SharedGrid[Shared Selenium Grid<br/>100 Chrome + 50 Firefox]
        end

        RDS1[(RDS PostgreSQL<br/>Test Results)]
        S3East[S3 Bucket<br/>Test Artifacts<br/>us-east-1]
    end

    subgraph Region US-West
        subgraph EKS-Perf-Cluster
            LoadGen[Load Generator<br/>k6/JMeter Pods]
            PerfMonitor[Performance Monitor<br/>Grafana/Prometheus]
        end

        S3West[S3 Bucket<br/>Performance Data<br/>us-west-2]
    end

    subgraph Region EU-Central
        subgraph EKS-EU-Cluster
            EUTests[EU Test Namespace<br/>GDPR Compliance Tests]
            EUGrid[EU Selenium Grid<br/>Data Residency]
        end

        RDS2[(RDS PostgreSQL<br/>EU Test Results)]
    end

    subgraph Global Services
        Route53[Route 53<br/>DNS Load Balancing]
        CloudFront[CloudFront CDN<br/>Dashboard Distribution]
        IAM[IAM Roles<br/>Cross-Cluster Auth]
        ECR[Elastic Container Registry<br/>Test Images]
    end

    subgraph Control Plane
        Argo[ArgoCD<br/>GitOps Deployment]
        Flux[Flux<br/>Config Sync]
        Rancher[Rancher<br/>Multi-Cluster Management]
    end

    Route53 --> ProdTests
    Route53 --> TestNS1
    Route53 --> EUTests

    Argo --> EKS-Prod-Cluster
    Argo --> EKS-Test-Cluster
    Argo --> EKS-EU-Cluster
    Flux --> EKS-Perf-Cluster

    Rancher --> EKS-Prod-Cluster
    Rancher --> EKS-Test-Cluster
    Rancher --> EKS-Perf-Cluster
    Rancher --> EKS-EU-Cluster

    TestNS1 --> RDS1
    TestNS2 --> RDS1
    TestNS3 --> RDS1
    ProdTests --> RDS1

    EUTests --> RDS2

    TestNS1 --> S3East
    SharedGrid --> S3East
    LoadGen --> S3West
    EUGrid --> S3East

    TestNS1 --> ECR
    SharedGrid --> ECR
    LoadGen --> ECR

    CloudFront --> Rancher

    style EKS-Prod-Cluster fill:#FFE4E1
    style EKS-Test-Cluster fill:#E0F7FA
    style EKS-Perf-Cluster fill:#FFF9C4
    style EKS-EU-Cluster fill:#E8F5E9
    style SharedGrid fill:#87CEEB
```

### 1.6 State Diagram - Test Execution Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Submitted: Test submitted

    Submitted --> Queued: Pass validation
    Submitted --> Rejected: Fail validation
    Rejected --> [*]

    Queued --> Scheduled: Resources available
    Queued --> Waiting: Resources unavailable
    Waiting --> Scheduled: Resources freed
    Waiting --> Cancelled: Timeout/User cancellation

    Scheduled --> Initializing: Pod created
    Initializing --> Running: Container started
    Initializing --> Failed: Init failure

    Running --> Completed: All tests passed
    Running --> Failed: Test failures
    Running --> Timeout: Execution timeout
    Running --> Cancelled: User cancellation

    Failed --> Retrying: Retry available
    Timeout --> Retrying: Retry available
    Retrying --> Scheduled: Retry scheduled
    Retrying --> Failed: Max retries exceeded

    Completed --> Cleanup: Collect results
    Failed --> Cleanup: Collect failure logs
    Cancelled --> Cleanup: Collect partial results

    Cleanup --> [*]

    note right of Queued
        Priority queue
        FIFO within priority
    end note

    note right of Running
        Logs streamed to
        ELK stack in real-time
    end note

    note right of Cleanup
        TTL: 1 hour
        Then pod deleted
    end note
```

---

## Architecture #2: Event-Driven + Chaos Engineering {#event-driven-chaos}

### 2.1 Class Diagram - Event-Driven Test Framework

```mermaid
classDiagram
    class EventDrivenTestFramework {
        -EventBroker broker
        -ChaosEngine chaosEngine
        -EventStore eventStore
        -TestOrchestrator orchestrator
        +registerTest(EventTest) void
        +executeChaosExperiment(ChaosExperiment) ExperimentResult
        +replayEvents(string aggregateId) void
    }

    class EventTest {
        -string testId
        -string name
        -EventTrigger trigger
        -List~EventAssertion~ assertions
        -ChaosScenario chaosScenario
        -int timeout
        +execute() TestResult
        +validate() boolean
        +setupChaos() void
        +teardownChaos() void
    }

    class EventBroker {
        <<interface>>
        +subscribe(string topic, EventHandler) Subscription
        +publish(Event) void
        +createConsumerGroup(string groupId) ConsumerGroup
        +getConsumerLag() Map~string,long~
    }

    class KafkaBroker {
        -KafkaProducer producer
        -KafkaConsumer consumer
        -AdminClient adminClient
        +subscribe(string topic, EventHandler) Subscription
        +publish(Event) void
        +seekToBeginning() void
        +seekToTimestamp(long timestamp) void
    }

    class RabbitMQBroker {
        -Connection connection
        -Channel channel
        -Exchange exchange
        +subscribe(string topic, EventHandler) Subscription
        +publish(Event) void
        +declareQueue(QueueConfig) Queue
        +bindQueue(string queue, string routingKey) void
    }

    class NATSBroker {
        -Connection nc
        -JetStream js
        +subscribe(string topic, EventHandler) Subscription
        +publish(Event) void
        +createStream(StreamConfig) Stream
        +getStreamInfo() StreamInfo
    }

    class Event {
        -string eventId
        -string eventType
        -string aggregateId
        -long timestamp
        -Map~string,Object~ payload
        -Map~string,string~ metadata
        +validate() boolean
        +serialize() byte[]
        +deserialize(byte[]) Event
    }

    class EventAssertion {
        -string eventType
        -Predicate~Event~ condition
        -int timeoutMs
        +evaluate(Event) AssertionResult
        +waitForEvent() Event
    }

    class EventHandler {
        <<interface>>
        +handle(Event) void
        +onError(Exception) void
    }

    class TestEventHandler {
        -List~Event~ receivedEvents
        -CountDownLatch latch
        -EventMatcher matcher
        +handle(Event) void
        +waitForEvents(int count, int timeout) List~Event~
        +getReceivedEvents() List~Event~
    }

    class ChaosEngine {
        -List~ChaosProvider~ providers
        -ExperimentScheduler scheduler
        -SteadyStateValidator validator
        -RollbackManager rollbackMgr
        +executeExperiment(ChaosExperiment) ExperimentResult
        +scheduleExperiment(ChaosExperiment, Schedule) void
        +abortExperiment(string experimentId) void
        +getExperimentStatus(string experimentId) ExperimentStatus
    }

    class ChaosExperiment {
        -string experimentId
        -string name
        -TargetSelector targetSelector
        -FaultInjection fault
        -SteadyStateHypothesis hypothesis
        -int duration
        -BlastRadiusLimit blastRadius
        +validate() boolean
        +toKubernetesCRD() CustomResource
        +execute() ExperimentResult
    }

    class ChaosProvider {
        <<interface>>
        +getName() string
        +getSupportedFaults() List~FaultType~
        +injectFault(FaultInjection, TargetSelector) void
        +removeFault(string faultId) void
    }

    class LitmusChaosProvider {
        -KubernetesClient k8sClient
        +injectFault(FaultInjection, TargetSelector) void
        +createChaosEngine(ChaosExperiment) ChaosEngine
        +monitorExperiment(string engineId) ExperimentStatus
    }

    class GremlinChaosProvider {
        -GremlinApiClient apiClient
        -ApiKey apiKey
        +injectFault(FaultInjection, TargetSelector) void
        +createAttack(AttackConfig) Attack
        +haltAttack(string attackId) void
    }

    class AWSFaultInjectionProvider {
        -FisClient fisClient
        -IamRole role
        +injectFault(FaultInjection, TargetSelector) void
        +startExperiment(ExperimentTemplate) Experiment
        +stopExperiment(string experimentId) void
    }

    class FaultInjection {
        -FaultType type
        -Map~string,Object~ parameters
        -TargetSelector target
        +getPodDeleteConfig() PodDeleteConfig
        +getNetworkLatencyConfig() NetworkLatencyConfig
        +getResourceStressConfig() ResourceStressConfig
    }

    class SteadyStateHypothesis {
        -List~HealthCheck~ healthChecks
        -SLODefinition slo
        -int toleranceThreshold
        +validate() boolean
        +check() SteadyStateResult
    }

    class AIChaosSuggestionEngine {
        -DependencyGraphAnalyzer graphAnalyzer
        -TrafficPatternAnalyzer trafficAnalyzer
        -HistoricalFailureDB failureDB
        -MLModel predictionModel
        +analyzeArchitecture() ArchitectureInsights
        +suggestChaosTargets() List~ChaosTarget~
        +predictImpact(ChaosExperiment) ImpactPrediction
        +learnFromExperiment(ExperimentResult) void
    }

    class EventStore {
        -Database database
        -EventSerializer serializer
        +append(Event) void
        +readStream(string aggregateId) List~Event~
        +readFromPosition(long position) List~Event~
        +subscribeToAll(EventHandler) Subscription
    }

    class SagaCoordinator {
        -Map~string,SagaInstance~ activeSagas
        -EventBroker broker
        +startSaga(SagaDefinition, Event) SagaInstance
        +compensate(string sagaId) void
        +getSagaStatus(string sagaId) SagaStatus
    }

    EventDrivenTestFramework --> EventBroker
    EventDrivenTestFramework --> ChaosEngine
    EventDrivenTestFramework --> EventStore
    EventDrivenTestFramework --> EventTest

    EventBroker <|-- KafkaBroker
    EventBroker <|-- RabbitMQBroker
    EventBroker <|-- NATSBroker

    EventTest --> EventAssertion
    EventTest --> ChaosExperiment
    EventAssertion --> Event

    EventHandler <|-- TestEventHandler
    TestEventHandler --> Event

    ChaosEngine --> ChaosProvider
    ChaosEngine --> ChaosExperiment
    ChaosEngine --> SteadyStateHypothesis
    ChaosEngine --> AIChaosSuggestionEngine

    ChaosProvider <|-- LitmusChaosProvider
    ChaosProvider <|-- GremlinChaosProvider
    ChaosProvider <|-- AWSFaultInjectionProvider

    ChaosExperiment --> FaultInjection
    ChaosExperiment --> SteadyStateHypothesis

    EventStore --> Event
```

### 2.2 Data Flow Diagram - Event-Driven Test with Chaos Injection

```mermaid
flowchart TD
    Start([Test Trigger]) --> InitTest[Initialize Event-Driven Test]
    InitTest --> SetupConsumer[Setup Event Consumer<br/>Subscribe to Topics]
    SetupConsumer --> CheckSteadyState{Check Steady<br/>State Baseline}

    CheckSteadyState -->|Unhealthy| Abort1[Abort Test]
    CheckSteadyState -->|Healthy| StartChaos[Start Chaos Experiment]

    StartChaos --> SelectTarget[AI Selects Chaos Target<br/>Based on Architecture]
    SelectTarget --> InjectFault[Inject Fault]

    InjectFault --> FaultType{Fault Type}

    FaultType -->|Pod Kill| KillPod[Terminate Pod<br/>via Kubernetes API]
    FaultType -->|Network| NetworkFault[Inject Network Latency<br/>via Toxiproxy/tc]
    FaultType -->|Resource| ResourceFault[Stress CPU/Memory<br/>via stress-ng]
    FaultType -->|Application| AppFault[Throw Exception<br/>via Bytecode Injection]

    KillPod --> TriggerEvent
    NetworkFault --> TriggerEvent
    ResourceFault --> TriggerEvent
    AppFault --> TriggerEvent

    TriggerEvent[Trigger Business Event<br/>e.g., Order Created] --> PublishEvent[Publish Event to Broker]
    PublishEvent --> Broker{Event Broker}

    Broker -->|Kafka| KafkaTopic[(Kafka Topic<br/>orders.created)]
    Broker -->|RabbitMQ| RabbitQueue[(RabbitMQ Exchange<br/>order-events)]
    Broker -->|NATS| NATSStream[(NATS JetStream<br/>ORDER_CREATED)]

    KafkaTopic --> Consumers[Event Consumers<br/>Microservices]
    RabbitQueue --> Consumers
    NATSStream --> Consumers

    Consumers --> ProcessEvent[Process Event<br/>Business Logic]
    ProcessEvent --> ChaosSide{Chaos Impact}

    ChaosSide -->|Delayed| DelayedProcess[Processing Delayed<br/>Due to Network Latency]
    ChaosSide -->|Failed| FailedProcess[Processing Failed<br/>Service Unavailable]
    ChaosSide -->|Success| SuccessProcess[Processing Succeeded<br/>with Resilience]

    DelayedProcess --> PublishResult[Publish Result Event]
    FailedProcess --> PublishCompensation[Publish Compensation Event<br/>Saga Rollback]
    SuccessProcess --> PublishResult

    PublishResult --> TestConsumer[Test Event Consumer<br/>Listening for Results]
    PublishCompensation --> TestConsumer

    TestConsumer --> CollectEvents[Collect Received Events]
    CollectEvents --> EventStore[(Event Store<br/>Complete Event History)]

    EventStore --> AssertResults{Assert Test<br/>Expectations}

    AssertResults -->|Event Received| CheckTiming{Timing<br/>Within SLA?}
    AssertResults -->|Event Missing| CheckTimeout{Timeout?}
    AssertResults -->|Compensation OK| CompensationSuccess[Saga Compensated<br/>Correctly]

    CheckTiming -->|Yes| Pass[Test Passed]
    CheckTiming -->|No| SLABreach[SLA Breach Detected]
    CheckTimeout -->|No| AssertResults
    CheckTimeout -->|Yes| Fail[Test Failed]

    CompensationSuccess --> Pass
    SLABreach --> Fail

    Pass --> RemoveChaos[Remove Chaos Fault]
    Fail --> RemoveChaos

    RemoveChaos --> CheckSteadyState2{Verify Steady<br/>State Restored}
    CheckSteadyState2 -->|Restored| CollectMetrics[Collect Chaos Metrics]
    CheckSteadyState2 -->|Not Restored| Alert[Alert SRE Team<br/>System Unstable]

    CollectMetrics --> AILearn[AI Learns from Experiment<br/>Update Prediction Model]
    AILearn --> GenerateReport[Generate Test Report]

    GenerateReport --> PublishMetrics[Publish Metrics<br/>Prometheus]
    PublishMetrics --> SendTrace[Send Distributed Trace<br/>Jaeger/Zipkin]
    SendTrace --> End([Test Complete])

    Alert --> ManualIntervention[Manual Intervention Required]
    Abort1 --> End

    subgraph Chaos Layer
        SelectTarget
        InjectFault
        FaultType
        KillPod
        NetworkFault
        ResourceFault
        AppFault
        RemoveChaos
    end

    subgraph Event Streaming Layer
        Broker
        KafkaTopic
        RabbitQueue
        NATSStream
    end

    subgraph Microservices
        Consumers
        ProcessEvent
    end

    subgraph Test Framework
        TestConsumer
        CollectEvents
        AssertResults
    end

    subgraph Observability
        EventStore
        PublishMetrics
        SendTrace
        AILearn
    end

    style Start fill:#90EE90
    style End fill:#FFB6C1
    style Fail fill:#FF6B6B
    style Pass fill:#90EE90
    style Chaos Layer fill:#FFE4E1
    style Event Streaming Layer fill:#E0F7FA
    style Microservices fill:#FFF9C4
    style Test Framework fill:#E8F5E9
```

### 2.3 Sequence Diagram - Event-Driven Saga Test with Chaos

```mermaid
sequenceDiagram
    actor Tester
    participant TestFW as Test Framework
    participant AI as AI Chaos Engine
    participant Chaos as Chaos Provider<br/>(Litmus/Gremlin)
    participant K8s as Kubernetes
    participant Producer as Event Producer<br/>(Order Service)
    participant Kafka as Kafka Broker
    participant Consumer1 as Payment Service
    participant Consumer2 as Inventory Service
    participant Consumer3 as Fulfillment Service
    participant EventStore as Event Store
    participant Monitoring as Prometheus/Jaeger

    Tester->>TestFW: Start Saga Test<br/>"Order Processing with Chaos"
    TestFW->>EventStore: Subscribe to all events
    TestFW->>Monitoring: Check steady-state SLOs
    Monitoring-->>TestFW: System healthy (99.9% uptime)

    TestFW->>AI: Suggest chaos target
    AI->>AI: Analyze dependency graph
    AI->>AI: Check traffic patterns
    AI->>AI: Review historical failures
    AI-->>TestFW: Target: Payment Service<br/>Fault: Pod termination

    TestFW->>Chaos: Inject chaos: Kill Payment Pod
    Chaos->>K8s: Delete pod payment-svc-xyz
    K8s->>K8s: Terminate pod (grace period 30s)
    K8s-->>Chaos: Pod deleted
    Chaos-->>TestFW: Chaos active

    Note over TestFW,Consumer3: Chaos is now active - Payment service degraded

    TestFW->>Producer: Trigger test event<br/>POST /orders
    Producer->>Producer: Create Order ID: 12345
    Producer->>Kafka: Publish OrderCreated event
    Kafka-->>Producer: Ack (offset: 1234)
    Producer-->>TestFW: 202 Accepted

    Kafka->>Consumer1: Consume OrderCreated
    Kafka->>Consumer2: Consume OrderCreated
    Kafka->>Consumer3: Consume OrderCreated

    Consumer1->>Consumer1: Process payment for Order 12345
    Note over Consumer1: Pod is being terminated!
    Consumer1->>Consumer1: Connection lost
    Consumer1->>Kafka: Publish PaymentFailed event

    Consumer2->>Consumer2: Reserve inventory
    Consumer2->>Kafka: Publish InventoryReserved event

    Kafka->>Consumer3: Consume InventoryReserved
    Consumer3->>Consumer3: Check payment status

    Kafka->>Consumer3: Consume PaymentFailed
    Consumer3->>Consumer3: Saga compensation required
    Consumer3->>Kafka: Publish CancelFulfillment event

    Kafka->>Consumer2: Consume CancelFulfillment
    Consumer2->>Consumer2: Release inventory (compensate)
    Consumer2->>Kafka: Publish InventoryReleased event

    Kafka->>Producer: Consume InventoryReleased
    Producer->>Producer: Mark order as failed
    Producer->>Kafka: Publish OrderFailed event

    EventStore->>EventStore: Collect all events
    TestFW->>EventStore: Get events for Order 12345
    EventStore-->>TestFW: [OrderCreated, PaymentFailed,<br/>InventoryReserved, CancelFulfillment,<br/>InventoryReleased, OrderFailed]

    TestFW->>TestFW: Assert saga compensation
    TestFW->>TestFW: Verify event ordering
    TestFW->>TestFW: Check no duplicate events

    Note over K8s: Kubernetes auto-recovery
    K8s->>K8s: Detect pod missing
    K8s->>K8s: Create new payment pod
    K8s-->>Consumer1: payment-svc-abc started
    Consumer1->>Monitoring: Register healthy

    TestFW->>Chaos: Remove chaos injection
    Chaos->>K8s: Cleanup chaos resources
    Chaos-->>TestFW: Chaos removed

    TestFW->>Monitoring: Verify steady-state restored
    Monitoring-->>TestFW: System recovered (99.8% uptime)

    TestFW->>AI: Report experiment results
    AI->>AI: Learn from failure patterns
    AI->>AI: Update prediction model

    TestFW->>Monitoring: Push test metrics
    TestFW->>Monitoring: Send distributed trace
    TestFW-->>Tester: ✓ Test Passed<br/>Saga compensated correctly<br/>Recovery time: 45s
```

### 2.4 Activity Diagram - Chaos Engineering Workflow

```mermaid
flowchart TD
    Start([Start Chaos Program]) --> DefineHyp[Define Steady-State Hypothesis<br/>e.g., 99.9% availability, p99 < 200ms]

    DefineHyp --> IdentifyWeaknesses[Identify Potential Weaknesses]
    IdentifyWeaknesses --> ManualId[Manual: Architecture Review]
    IdentifyWeaknesses --> AIId[AI: Dependency Graph Analysis]

    ManualId --> CreateExp[Create Chaos Experiment]
    AIId --> CreateExp

    CreateExp --> DefineScope{Define Blast Radius}
    DefineScope -->|Dev| DevScope[Scope: Dev Namespace<br/>Impact: Low]
    DefineScope -->|Staging| StagingScope[Scope: Staging Cluster<br/>Impact: Medium]
    DefineScope -->|Prod| ProdScope[Scope: Canary Pods Only<br/>Impact: Controlled]

    DevScope --> DefineFault
    StagingScope --> DefineFault
    ProdScope --> DefineFault

    DefineFault[Define Fault Injection] --> InfraFault{Fault Category}

    InfraFault -->|Infrastructure| InfraTypes[Pod Kill / Network / DNS]
    InfraFault -->|Application| AppTypes[Exception / Latency / Error Response]
    InfraFault -->|Data| DataTypes[Event Duplication / Out-of-Order]
    InfraFault -->|Resource| ResourceTypes[CPU/Memory Stress]

    InfraTypes --> SetHalt
    AppTypes --> SetHalt
    DataTypes --> SetHalt
    ResourceTypes --> SetHalt

    SetHalt[Set Halt Conditions<br/>Auto-abort on SLO breach] --> SetDuration[Set Experiment Duration<br/>5-60 minutes]

    SetDuration --> PeerReview{Peer Review}
    PeerReview -->|Changes Requested| CreateExp
    PeerReview -->|Approved| ScheduleExp

    ScheduleExp[Schedule Experiment] --> GameDay{Type}
    GameDay -->|Automated| CICDGate[Run in CI/CD Pipeline]
    GameDay -->|Scheduled| CronJob[Cron: Every 4 hours]
    GameDay -->|Game Day| LiveEvent[Live Team Exercise]

    CICDGate --> PreCheck
    CronJob --> PreCheck
    LiveEvent --> PreCheck

    PreCheck[Pre-Experiment Checks] --> CheckOncall{On-Call<br/>Available?}
    CheckOncall -->|No| Postpone[Postpone Experiment]
    CheckOncall -->|Yes| CheckIncidents{Active<br/>Incidents?}
    CheckIncidents -->|Yes| Postpone
    CheckIncidents -->|No| CheckDeployments{Recent<br/>Deployments?}
    CheckDeployments -->|Yes < 2h| Postpone
    CheckDeployments -->|No| MeasureBaseline

    MeasureBaseline[Measure Baseline Metrics<br/>5 minute window] --> RunExperiment[Execute Chaos Experiment]

    RunExperiment --> MonitorReal[Real-time Monitoring]
    MonitorReal --> CheckMetrics{Metrics Status}

    CheckMetrics -->|SLO OK| ContinueChaos[Continue Experiment]
    CheckMetrics -->|SLO Breach| AutoAbort[Auto-Abort Experiment]
    CheckMetrics -->|Critical Alert| AutoAbort

    ContinueChaos --> TimeCheck{Duration<br/>Complete?}
    TimeCheck -->|No| MonitorReal
    TimeCheck -->|Yes| StopChaos[Stop Chaos Injection]

    AutoAbort --> StopChaos

    StopChaos --> ObserveRecovery[Observe System Recovery]
    ObserveRecovery --> RecoveryCheck{System<br/>Recovered?}

    RecoveryCheck -->|Yes| MeasureImpact[Measure Impact Metrics]
    RecoveryCheck -->|No, Wait| ObserveRecovery
    RecoveryCheck -->|No, Timeout| Escalate[Escalate to SRE]

    MeasureImpact --> CalculateBlastRadius[Calculate Actual Blast Radius<br/>Affected users, services, requests]
    CalculateBlastRadius --> CompareHypothesis{Hypothesis<br/>Validated?}

    CompareHypothesis -->|Yes| SuccessReport[Success: System Resilient]
    CompareHypothesis -->|No| FailureReport[Failure: Weakness Found]

    SuccessReport --> DocumentLearning
    FailureReport --> CreateJira[Create Jira Ticket<br/>Assign to Team]

    CreateJira --> DocumentLearning[Document Learning]
    DocumentLearning --> UpdateRunbook[Update Incident Runbook]
    UpdateRunbook --> ShareResults[Share Results<br/>Slack/Email/Wiki]

    ShareResults --> UpdateAI[Update AI Model<br/>with Experiment Data]
    UpdateAI --> NextExp{More<br/>Experiments?}

    NextExp -->|Yes| CreateExp
    NextExp -->|No| End([End])

    Postpone --> ScheduleRetry[Schedule Retry<br/>Next Available Slot]
    ScheduleRetry --> End

    Escalate --> PostMortem[Conduct Post-Mortem]
    PostMortem --> End

    style Start fill:#90EE90
    style End fill:#FFB6C1
    style AutoAbort fill:#FF6B6B
    style SuccessReport fill:#90EE90
    style FailureReport fill:#FFD700
    style Escalate fill:#FF6B6B
```

### 2.5 Component Diagram - Event-Driven + Chaos Architecture

```mermaid
graph TB
    subgraph Test Orchestration Layer
        TestDSL[Test DSL Engine<br/>Define event-driven tests]
        TestRunner[Test Runner<br/>Execute async tests]
        AssertionEngine[Assertion Engine<br/>Event matching & timing]
    end

    subgraph Chaos Control Plane
        ChaosAPI[Chaos API<br/>REST + gRPC]
        ChaosScheduler[Chaos Scheduler<br/>Cron + Event-triggered]
        AIEngine[AI Chaos Engine<br/>Target Selection]
        ExperimentDB[(Experiment Database<br/>History + Results)]
    end

    subgraph Chaos Providers
        Litmus[LitmusChaos<br/>K8s-native]
        Gremlin[Gremlin<br/>Enterprise SaaS]
        AWSFIS[AWS FIS<br/>Cloud-native]
        Toxiproxy[Toxiproxy<br/>Network proxy]
    end

    subgraph Event Streaming Platform
        KafkaCluster[Kafka Cluster<br/>3 Brokers]
        SchemaRegistry[Schema Registry<br/>Avro/Protobuf]
        KafkaConnect[Kafka Connect<br/>CDC Pipelines]
        KSQL[ksqlDB<br/>Stream Processing]
    end

    subgraph Microservices Under Test
        OrderSvc[Order Service<br/>Event Producer]
        PaymentSvc[Payment Service<br/>Event Consumer]
        InventorySvc[Inventory Service<br/>Event Consumer]
        FulfillmentSvc[Fulfillment Service<br/>Saga Coordinator]
    end

    subgraph Service Mesh
        IstioCP[Istio Control Plane<br/>Pilot + Citadel]
        EnvoyProxies[Envoy Sidecars<br/>Traffic Management]
        FaultInjection[Istio Fault Injection<br/>Delay/Abort Rules]
    end

    subgraph Event Store & Replay
        EventStoreDB[(EventStoreDB<br/>Append-only Log)]
        Projections[Event Projections<br/>Read Models]
        ReplayEngine[Replay Engine<br/>Time Travel Queries]
    end

    subgraph Observability Platform
        PrometheusTS[(Prometheus<br/>Time-Series DB)]
        JaegerTracing[Jaeger<br/>Distributed Tracing]
        GrafanaDash[Grafana<br/>Dashboards]
        AlertManager[Alert Manager<br/>PagerDuty Integration]
    end

    subgraph Test Data Management
        TestDataGen[Test Data Generator<br/>Faker + Templates]
        DataSeeder[Data Seeder<br/>Database Init]
        Idempotency[Idempotency Checker<br/>Duplicate Detection]
    end

    TestDSL --> TestRunner
    TestRunner --> AssertionEngine
    TestRunner --> ChaosAPI

    ChaosAPI --> ChaosScheduler
    ChaosAPI --> AIEngine
    ChaosScheduler --> ExperimentDB
    AIEngine --> ExperimentDB

    ChaosAPI --> Litmus
    ChaosAPI --> Gremlin
    ChaosAPI --> AWSFIS
    ChaosAPI --> Toxiproxy

    Litmus --> OrderSvc
    Litmus --> PaymentSvc
    Gremlin --> InventorySvc
    Toxiproxy --> KafkaCluster

    TestRunner --> TestDataGen
    TestDataGen --> DataSeeder
    DataSeeder --> OrderSvc

    OrderSvc --> EnvoyProxies
    PaymentSvc --> EnvoyProxies
    InventorySvc --> EnvoyProxies
    FulfillmentSvc --> EnvoyProxies

    EnvoyProxies --> IstioCP
    IstioCP --> FaultInjection

    OrderSvc --> KafkaCluster
    PaymentSvc --> KafkaCluster
    InventorySvc --> KafkaCluster
    FulfillmentSvc --> KafkaCluster

    KafkaCluster --> SchemaRegistry
    KafkaCluster --> KSQL
    KafkaConnect --> KafkaCluster

    TestRunner --> KafkaCluster
    AssertionEngine --> KafkaCluster

    KafkaCluster --> EventStoreDB
    EventStoreDB --> Projections
    EventStoreDB --> ReplayEngine
    TestRunner --> ReplayEngine

    OrderSvc --> PrometheusTS
    PaymentSvc --> PrometheusTS
    Litmus --> PrometheusTS
    Gremlin --> PrometheusTS

    OrderSvc --> JaegerTracing
    PaymentSvc --> JaegerTracing
    EnvoyProxies --> JaegerTracing

    PrometheusTS --> GrafanaDash
    PrometheusTS --> AlertManager
    JaegerTracing --> GrafanaDash

    AssertionEngine --> Idempotency

    style TestRunner fill:#87CEEB
    style ChaosAPI fill:#FFB6C1
    style AIEngine fill:#DDA0DD
    style KafkaCluster fill:#FFD700
    style EventStoreDB fill:#90EE90
```

### 2.6 State Diagram - Chaos Experiment Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Draft: Create experiment

    Draft --> UnderReview: Submit for review
    Draft --> Archived: Discard

    UnderReview --> Approved: Peer approved
    UnderReview --> ChangesRequested: Revisions needed
    ChangesRequested --> Draft: Update experiment

    Approved --> Scheduled: Schedule execution
    Scheduled --> WaitingForWindow: Wait for safe window

    WaitingForWindow --> PreFlightCheck: Window opened
    WaitingForWindow --> Cancelled: Window missed

    PreFlightCheck --> ReadyToRun: All checks passed
    PreFlightCheck --> Blocked: Pre-flight failed
    Blocked --> WaitingForWindow: Retry later

    ReadyToRun --> Running: Start chaos injection

    Running --> Monitoring: Inject fault

    state Monitoring {
        [*] --> CheckingSLO
        CheckingSLO --> SLOHealthy: Metrics OK
        CheckingSLO --> SLOBreached: Metrics degraded
        SLOHealthy --> CheckingSLO: Continue monitoring
        SLOBreached --> [*]: Trigger abort
    }

    Monitoring --> Completed: Duration elapsed
    Monitoring --> Aborted: SLO breach detected
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

    HypothesisConfirmed --> Completed_Success
    HypothesisRejected --> Completed_ActionRequired

    Completed_Success --> [*]
    Completed_ActionRequired --> [*]
    IncidentMode --> [*]
    Cancelled --> [*]
    Archived --> [*]

    note right of PreFlightCheck
        Checks:
        - No active incidents
        - On-call available
        - No recent deployments
        - System healthy
    end note

    note right of Monitoring
        Real-time checks:
        - Error rate < 1%
        - Latency p99 < 500ms
        - CPU < 80%
        - Consumer lag < 1000
    end note

    note right of Recovery
        Automatic rollback:
        - Remove fault injection
        - Restore network rules
        - Clear chaos labels
        - Wait for stabilization
    end note
```

---

## Hybrid Architecture: Combining Both Approaches {#hybrid}

### 3.1 Unified Architecture Diagram

```mermaid
graph TB
    subgraph CI/CD Pipeline
        GitOps[GitOps Repository<br/>Test Definitions + Config]
        CI[CI System<br/>GitHub Actions]
    end

    subgraph Kubernetes Test Orchestration
        TestAPI[Test Orchestration API]
        TestScheduler[K8s Test Scheduler]
        TestPods[Test Execution Pods]
    end

    subgraph Event-Driven Test Layer
        EventTests[Event-Driven Tests<br/>Kafka/NATS Consumers]
        SagaTests[Saga Pattern Tests<br/>Distributed Transactions]
        EventStore[(Event Store<br/>Complete History)]
    end

    subgraph Chaos Engineering Layer
        ChaosController[Chaos Controller]
        AIEngine[AI Target Selector]
        ChaosProviders[Multi-Provider<br/>Litmus + Gremlin + FIS]
    end

    subgraph Application Under Test
        ServiceMesh[Service Mesh<br/>Istio]

        subgraph Microservices
            APIGateway[API Gateway]
            OrderMS[Order Service]
            PaymentMS[Payment Service]
            InventoryMS[Inventory Service]
        end

        Kafka[Kafka Cluster<br/>Event Streaming]
        Databases[(Databases<br/>PostgreSQL/MongoDB)]
    end

    subgraph Observability
        Prometheus[Prometheus]
        Jaeger[Jaeger Tracing]
        Grafana[Grafana]
        ELK[ELK Stack]
    end

    subgraph Storage
        S3[Object Storage<br/>Artifacts]
        ResultsDB[(Test Results DB)]
    end

    GitOps --> CI
    CI --> TestAPI
    TestAPI --> TestScheduler
    TestScheduler --> TestPods
    TestScheduler --> EventTests

    TestPods --> APIGateway
    EventTests --> Kafka
    SagaTests --> Kafka

    TestAPI --> ChaosController
    ChaosController --> AIEngine
    AIEngine --> ChaosProviders
    ChaosProviders --> ServiceMesh
    ChaosProviders --> Microservices
    ChaosProviders --> Kafka

    ServiceMesh --> Microservices
    OrderMS --> Kafka
    PaymentMS --> Kafka
    InventoryMS --> Kafka

    Microservices --> Databases

    Kafka --> EventStore
    EventTests --> EventStore

    TestPods --> Prometheus
    EventTests --> Prometheus
    Microservices --> Prometheus
    ChaosProviders --> Prometheus

    TestPods --> Jaeger
    ServiceMesh --> Jaeger
    Microservices --> Jaeger

    Prometheus --> Grafana
    Jaeger --> Grafana

    TestPods --> ELK
    Microservices --> ELK

    TestPods --> S3
    EventTests --> ResultsDB
    SagaTests --> ResultsDB

    style TestPods fill:#87CEEB
    style EventTests fill:#90EE90
    style ChaosProviders fill:#FFB6C1
    style AIEngine fill:#DDA0DD
    style Kafka fill:#FFD700
```

### 3.2 Complete Test Execution Sequence (Hybrid)

```mermaid
sequenceDiagram
    actor QA as QA Engineer
    participant Git as Git Repository
    participant CI as GitHub Actions
    participant TestAPI as Test Orchestration API
    participant K8s as Kubernetes
    participant TestPod as Test Pod
    participant ChaosCtrl as Chaos Controller
    participant AI as AI Engine
    participant Litmus as LitmusChaos
    participant Kafka as Kafka Broker
    participant OrderSvc as Order Service
    participant PaymentSvc as Payment Service
    participant EventStore as Event Store
    participant Prometheus as Prometheus
    participant Slack as Slack

    QA->>Git: Push test suite update
    Git->>CI: Trigger workflow

    CI->>TestAPI: POST /test-suites/execute
    TestAPI->>TestAPI: Parse test definitions
    TestAPI->>K8s: Create test namespace

    par Parallel Test Execution
        TestAPI->>K8s: Create API test pods (50)
        K8s->>TestPod: Start pod-1...pod-50
        TestPod->>OrderSvc: POST /api/orders
        OrderSvc-->>TestPod: 201 Created
        TestPod->>Prometheus: Record API test metrics
    and Event-Driven Tests
        TestAPI->>K8s: Create event test pods (10)
        K8s->>TestPod: Start event-pod-1...10
        TestPod->>Kafka: Subscribe to order.events
        TestPod->>OrderSvc: POST /api/orders
        OrderSvc->>Kafka: Publish OrderCreated
        Kafka->>PaymentSvc: Consume OrderCreated
        Kafka->>TestPod: Consume OrderCreated
        TestPod->>EventStore: Verify event
        TestPod->>Prometheus: Record event metrics
    end

    Note over TestAPI,Litmus: Phase 2: Chaos Testing

    TestAPI->>ChaosCtrl: Start chaos experiments
    ChaosCtrl->>AI: Suggest chaos targets
    AI->>AI: Analyze service dependencies
    AI-->>ChaosCtrl: Target: PaymentSvc, Fault: PodKill

    ChaosCtrl->>Litmus: Create ChaosEngine CR
    Litmus->>K8s: Delete payment-svc pod
    K8s->>PaymentSvc: Terminate pod

    Note over PaymentSvc: Payment service down

    TestPod->>OrderSvc: POST /api/orders (during chaos)
    OrderSvc->>Kafka: Publish OrderCreated
    Kafka->>PaymentSvc: Consume OrderCreated (fails)
    Kafka->>Kafka: Message redelivery (retry)

    Note over K8s: K8s auto-recovery
    K8s->>PaymentSvc: Create new pod
    PaymentSvc->>Prometheus: Register healthy

    Kafka->>PaymentSvc: Redeliver OrderCreated
    PaymentSvc->>Kafka: Publish PaymentProcessed
    Kafka->>TestPod: Consume PaymentProcessed

    TestPod->>TestPod: Assert: Event received
    TestPod->>TestPod: Assert: Idempotency maintained
    TestPod->>TestPod: Assert: Recovery time < 60s

    TestPod->>EventStore: Store test events
    TestPod->>Prometheus: Push chaos test metrics

    ChaosCtrl->>Litmus: Stop chaos experiment
    Litmus->>Litmus: Cleanup chaos resources
    Litmus-->>ChaosCtrl: Experiment complete

    ChaosCtrl->>AI: Report experiment results
    AI->>AI: Update ML model

    TestPod->>TestAPI: Report test results
    TestAPI->>TestAPI: Aggregate all results
    TestAPI->>Prometheus: Push aggregated metrics
    TestAPI->>Slack: Send notification
    TestAPI-->>CI: Return test report

    CI->>Git: Update test status badge
    CI->>Slack: Post detailed report
    CI-->>QA: Workflow complete
```

### 3.3 Integration Points Matrix

```mermaid
graph LR
    subgraph Integration Patterns
        A[K8s Orchestration] -->|Triggers| B[Chaos Experiments]
        B -->|Monitors| C[Event Streams]
        C -->|Validates| A

        A -->|Provisions| D[Test Environments]
        D -->|Runs| E[Event-Driven Tests]
        E -->|Collects| F[Test Results]

        B -->|Injects Faults Into| G[Microservices]
        G -->|Produces| C
        C -->|Consumed By| E

        F -->|Feeds| H[AI Model]
        H -->|Optimizes| B
        H -->|Prioritizes| A

        I[Service Mesh] -->|Observes| G
        I -->|Controls Traffic| G
        I -->|Provides Metrics| F

        J[Observability] -->|Monitors| A
        J -->|Monitors| B
        J -->|Monitors| E
        J -->|Monitors| G
    end

    style A fill:#87CEEB
    style B fill:#FFB6C1
    style E fill:#90EE90
    style H fill:#DDA0DD
```

---

## Summary

This document provides comprehensive visual representations of both advanced test automation architectures:

### Kubernetes-Native Orchestration Diagrams
1. **Class Diagram**: Complete OOP design of test orchestration platform
2. **Data Flow Diagram**: End-to-end test execution pipeline
3. **Sequence Diagram**: Detailed interaction flow with timing
4. **Component Diagram**: Infrastructure layout with all components
5. **Deployment Diagram**: Multi-cluster, multi-region architecture
6. **State Diagram**: Test execution lifecycle states

### Event-Driven + Chaos Engineering Diagrams
1. **Class Diagram**: Event-driven test framework and chaos engine design
2. **Data Flow Diagram**: Event flows with chaos injection points
3. **Sequence Diagram**: Saga pattern test with chaos and recovery
4. **Activity Diagram**: Complete chaos engineering workflow
5. **Component Diagram**: Event streaming and chaos infrastructure
6. **State Diagram**: Chaos experiment lifecycle management

### Hybrid Architecture
1. **Unified Architecture**: Combined system showing all components
2. **Complete Sequence**: End-to-end test execution with both approaches
3. **Integration Matrix**: How components interact across architectures

These diagrams are rendered using Mermaid and can be viewed in any Markdown viewer that supports Mermaid syntax (GitHub, GitLab, VS Code with plugins, etc.).
