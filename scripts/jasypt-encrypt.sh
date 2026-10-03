#!/usr/bin/env bash
# Encrypt one value with the project's Jasypt settings.
#   ./scripts/jasypt-encrypt.sh 'the-secret'   ->   ENC(...)
#
# Uses the Gradle plugin rather than the standalone Jasypt CLI: the plugin shares
# jasypt-spring-boot's defaults, so there is no algorithm/IV/salt/iteration set to
# match by hand - which is the usual way an encrypted value fails at boot.
set -euo pipefail
: "${JASYPT_ENCRYPTOR_PASSWORD:?set JASYPT_ENCRYPTOR_PASSWORD first}"
cd "$(dirname "$0")/.."
./gradlew -q --console=plain encryptText \
  --password="$JASYPT_ENCRYPTOR_PASSWORD" --text="$1" --rerun \
  | awk '/^Encrypted text: /{print "ENC(" $3 ")"}'