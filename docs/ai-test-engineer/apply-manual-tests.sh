#!/bin/bash
# Run ONLY after the v1 PIT run is finished and v1 reports are committed.
set -e
DEST=src/test/java/com/yanimetaxas/bookkeeping/manual
mkdir -p "$DEST"
cp docs/ai-test-engineer/staged-manual-tests/*.java "$DEST"/
echo "Manual tests copied to $DEST. Commit them separately: git add $DEST && git commit -m 'Add manual tests (hybrid suite v1)'"
