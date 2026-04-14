#!/bin/sh
set -eu

git config core.hooksPath .githooks
echo "configured git hooks path to .githooks"
