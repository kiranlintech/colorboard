pipeline {

    agent any

    tools {
        maven 'Maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        APP_NAME = 'colorboard'

        BACKEND_IMAGE = 'colorboard-backend'

        COMPOSE_FILE = 'docker-compose.yml'
    }

    stages {

        /*
         * =========================================================
         * 1. CHECKOUT
         * =========================================================
         */
        stage('Checkout') {
            steps {
                echo 'Checking out ColorBoard source code...'

                checkout scm
            }
        }


        /*
         * =========================================================
         * 2. BUILD & TEST JAVA APPLICATION
         * =========================================================
         */
        stage('Maven Build & Test') {
            steps {

                dir('backend') {

                    echo 'Running Maven tests...'

                    sh '''
                        mvn clean test
                    '''
                }
            }
        }


        /*
         * =========================================================
         * 3. BUILD JAR
         * =========================================================
         */
        stage('Package Application') {
            steps {

                dir('backend') {

                    echo 'Creating Spring Boot JAR...'

                    sh '''
                        mvn clean package -DskipTests
                    '''
                }
            }
        }


        /*
         * =========================================================
         * 4. BUILD DOCKER IMAGE
         * =========================================================
         */
        stage('Build Docker Image') {
            steps {

                echo 'Building ColorBoard backend Docker image...'

                sh '''
                    docker build \
                        -t ${BACKEND_IMAGE}:${BUILD_NUMBER} \
                        -t ${BACKEND_IMAGE}:latest \
                        ./backend
                '''
            }
        }


        /*
         * =========================================================
         * 5. TRIVY SECURITY SCAN
         * =========================================================
         */
        stage('Security Scan - Trivy') {
            steps {

                echo 'Scanning Docker image for vulnerabilities...'

                sh '''
                    if command -v trivy >/dev/null 2>&1; then

                        trivy image \
                            --severity HIGH,CRITICAL \
                            --exit-code 1 \
                            ${BACKEND_IMAGE}:${BUILD_NUMBER}

                    else

                        echo "WARNING: Trivy is not installed."
                        echo "Skipping Trivy scan."

                    fi
                '''
            }
        }


        /*
         * =========================================================
         * 6. DEPLOY
         * =========================================================
         */
        stage('Deploy') {
            steps {

                echo 'Deploying ColorBoard using Docker Compose...'

                sh '''
                    docker compose -f ${COMPOSE_FILE} down

                    docker compose -f ${COMPOSE_FILE} up -d --build
                '''
            }
        }


        /*
         * =========================================================
         * 7. SMOKE TEST
         * =========================================================
         */
        stage('Smoke Test') {
            steps {

                echo 'Testing ColorBoard application...'

                sh '''
                    sleep 15

                    echo "Testing NGINX..."

                    curl -f http://localhost/ || exit 1

                    echo "Testing backend API..."

                    curl -f http://localhost/api/tasks/health || exit 1

                    echo ""
                    echo "ColorBoard deployment successful!"
                '''
            }
        }
    }


    /*
     * =============================================================
     * POST ACTIONS
     * =============================================================
     */
    post {

        success {

            echo '''
            ==========================================
             COLORBOARD DEPLOYMENT SUCCESSFUL
            ==========================================
            '''
        }

        failure {

            echo '''
            ==========================================
             COLORBOARD PIPELINE FAILED
            ==========================================
            '''
        }

        always {

            echo 'Cleaning Jenkins workspace...'

            cleanWs()
        }
    }
}
