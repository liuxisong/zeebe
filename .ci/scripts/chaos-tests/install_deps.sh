#!/bin/sh

set -e

echo "Installing chaostoolkit"
pip install chaostoolkit
chaos --version

echo "Installing bash"
apk update
apk add bash

echo "Installing kubectl"
apk add curl
kubectl_version=$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)
curl -LO https://storage.googleapis.com/kubernetes-release/release/${kubectl_version}/bin/linux/amd64/kubectl
chmod +x kubectl
mv kubectl /usr/local/bin
kubectl version --client
