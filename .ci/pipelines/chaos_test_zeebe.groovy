final PROJECT = "zeebe"
final CHAOS_TEST_NAMESPACE = "zeebe-chaos-test"

pipeline {
    agent {
        kubernetes {
            cloud "${PROJECT}-ci"
            label "${PROJECT}-ci-build-${env.JOB_BASE_NAME}-${env.BUILD_ID}"
            defaultContainer 'jnlp'
            yaml pythonAgent(PROJECT)
        }
    }
    stages {
        stage('Clone') {
            steps {
                dir('zeebe-benchmark') {
                    git url: 'git@github.com:zeebe-io/zeebe-benchmark',
                            branch: "master",
                            credentialsId: 'camunda-jenkins-github-ssh',
                            poll: false
                }
                dir('zeebe') {
                    git url: 'git@github.com:zeebe-io/zeebe',
                            branch: "SRE-668", // TODO: change me
                            credentialsId: 'camunda-jenkins-github-ssh',
                            poll: false
                }
            }
        }

        stage('Install dependencies') {
            steps {
                container('python') {
                    sh "zeebe/.ci/scripts/chaos-tests/install_deps.sh"
                }
            }
        }

        stage('Create cluster') {
            steps {
                container('python') {
                    sh "zeebe/.ci/scripts/chaos-tests/setup.sh ${CHAOS_TEST_NAMESPACE}"
                }
            }
        }

        // TODO create namespace
        // TODO set up the cluster
        // TODO remove
        // TODO remove namespace
        // TODO notify team
    }
}

static String pythonAgent(String namespace) {
    def nodePool = 'slaves'
    """
metadata:
  labels:
    agent: ${namespace}-ci-build
spec:
  nodeSelector:
    cloud.google.com/gke-nodepool: ${nodePool}
  tolerations:
    - key: "${nodePool}"
      operator: "Exists"
      effect: "NoSchedule"
  serviceAccountName: stage-ci-${namespace}-camunda-cloud # TODO change me
  containers:
  - name: python
    image: python:3.8-alpine
    imagePullPolicy: Always
    command: ["cat"]
    tty: true
    resources:
      limits:
        cpu: 1
        memory: 512Mi
      requests:
        cpu: 200m
        memory: 128Mi
"""
}
