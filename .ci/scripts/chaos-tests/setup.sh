#!/bin/bash

set -euo pipefail

fail() {
  echo "[${0}] Error: ${1}"
  exit 1
}

NAMESPACE="${1:-}"

[[ -z "${NAMESPACE}" ]] && fail "Namespace name is not passed."
if ! [[ "${NAMESPACE}" =~ ^zeebe-chaos.* ]]; then
  fail "Namespace must start with 'zeebe-chaos'"
fi

#set +e
#kubectl get namespace ${NAMESPACE} 2> /dev/null
#if [[ $? -eq 0 ]]; then
#  fail "Namespace ${NAMESPACE} already exists. Is the previous test still running?"
#fi
#set -e

#kubectl config get-contexts
kubectl get deployments
# Create NS
#kubectl create namespace ${NAMESPACE}
# Change NS
#kubectl config set-context --current --namespace ${NAMESPACE}
# List pods
kubectl get deployments -n ${NAMESPACE}
