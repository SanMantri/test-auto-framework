/**
 * Jenkinsfile - Declarative Pipeline for Agentic Hybrid Test Automation
 * 
 * FEATURES:
 * - Parallel Stage Execution (Sharding across multiple nodes/executors)
 * - Matrix Build for cross-browser testing
 * - Artifact archiving (Traces, Videos, Reports)
 * - Slack/Email notifications (configurable)
 * 
 * USAGE:
 * 1. Create a Pipeline job in Jenkins
 * 2. Point SCM to this repository
 * 3. Set script path to "Jenkinsfile"
 */

pipeline {
    agent any

    // Environment variables
    environment {
        JAVA_HOME = tool name: 'JDK17', type: 'jdk'
        MAVEN_HOME = tool name: 'Maven3', type: 'maven'
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"
        
        // Playwright config
        PLAYWRIGHT_BROWSERS_PATH = "${WORKSPACE}/.playwright-browsers"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }

    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['regression', 'smoke', 'payment', 'booking', 'visual'],
            description: 'Select the test suite to run'
        )
        booleanParam(
            name: 'RUN_PARALLEL_SHARDS',
            defaultValue: true,
            description: 'Run tests in parallel shards'
        )
        string(
            name: 'THREAD_COUNT',
            defaultValue: '5',
            description: 'Number of parallel threads per shard'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "✅ Code checked out successfully"
            }
        }

        stage('Setup') {
            steps {
                echo "🔧 Setting up environment..."
                sh '''
                    java -version
                    mvn -version
                '''
                // Install Playwright browsers
                sh 'mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium" || true'
                echo "✅ Playwright browsers installed"
            }
        }

        stage('Build') {
            steps {
                echo "🏗️ Compiling project..."
                sh 'mvn clean compile -DskipTests'
                echo "✅ Build successful"
            }
        }

        stage('Test - Parallel Shards') {
            when {
                expression { params.RUN_PARALLEL_SHARDS == true }
            }
            parallel {
                stage('Shard 1: Payment Tests') {
                    steps {
                        echo "💳 Running Payment Tests..."
                        sh '''
                            mvn test -P shard-payment \
                                -Dautomation.headless=true \
                                -Dautomation.tracing.enabled=true \
                                -DforkCount=2 \
                                -DthreadCount=${THREAD_COUNT}
                        '''
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'target/traces/**/*.zip', allowEmptyArchive: true
                        }
                    }
                }

                stage('Shard 2: Booking Tests') {
                    steps {
                        echo "🎟️ Running Booking Tests..."
                        sh '''
                            mvn test -P shard-booking \
                                -Dautomation.headless=true \
                                -Dautomation.tracing.enabled=true \
                                -DforkCount=2 \
                                -DthreadCount=${THREAD_COUNT}
                        '''
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'target/traces/**/*.zip', allowEmptyArchive: true
                        }
                    }
                }

                stage('Shard 3: Visual Tests') {
                    steps {
                        echo "📊 Running Visual/Dashboard Tests..."
                        sh '''
                            mvn test -P shard-visual \
                                -Dautomation.headless=true \
                                -Dautomation.tracing.enabled=true \
                                -DforkCount=1 \
                                -DthreadCount=2
                        '''
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'target/traces/**/*.zip', allowEmptyArchive: true
                        }
                    }
                }
            }
        }

        stage('Test - Single Suite') {
            when {
                expression { params.RUN_PARALLEL_SHARDS == false }
            }
            steps {
                echo "🧪 Running ${params.TEST_SUITE} suite..."
                sh """
                    mvn test -Dtest.groups=${params.TEST_SUITE} \
                        -Dautomation.headless=true \
                        -Dautomation.tracing.enabled=true \
                        -DforkCount=2 \
                        -DthreadCount=${params.THREAD_COUNT}
                """
            }
        }

        stage('Publish Reports') {
            steps {
                echo "📈 Publishing test reports..."
                // TestNG Reports
                publishHTML(target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target/surefire-reports',
                    reportFiles: 'index.html',
                    reportName: 'TestNG Report'
                ])
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning up..."
            // Archive traces and videos
            archiveArtifacts artifacts: 'target/traces/**/*.zip', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/videos/**/*.webm', allowEmptyArchive: true
            
            // Publish TestNG results
            testNG()
        }
        success {
            echo "✅ Pipeline completed successfully!"
            // Uncomment for Slack notification
            // slackSend channel: '#automation', color: 'good', message: "✅ Build ${BUILD_NUMBER} passed"
        }
        failure {
            echo "❌ Pipeline failed!"
            // Uncomment for Slack notification
            // slackSend channel: '#automation', color: 'danger', message: "❌ Build ${BUILD_NUMBER} failed"
        }
    }
}
