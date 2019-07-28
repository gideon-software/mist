#!/bin/bash

set -e

PACKAGER=${1}
INSTALLER_TYPE=${2}
APP_NAME=${3}
INPUT=${4}
OUTPUT=${5}
JAVA_RUNTIME=${6}
JAR=${7}
VERSION=${8}
APP_ICON=${9}
EXTRA_BUNDLER_ARGUMENTS=${10}

${PACKAGER} \
  create-installer ${INSTALLER_TYPE} \
  --verbose \
  --echo-mode \
  --name "${APP_NAME}" \
  --main-jar "${JAR}" \
  --version "${VERSION}" \
  --input "${INPUT}" \
  --output "${OUTPUT}" \
  --runtime-image "${JAVA_RUNTIME}" \
  --icon "${APP_ICON}" \
  ${EXTRA_BUNDLER_ARGUMENTS}