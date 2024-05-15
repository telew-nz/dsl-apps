#!/usr/bin/bash

eval "$(ssh-agent -s)"
ssh-add $GITHUB_SSH_PATH
git config user.name = "DSL Sync Service"
git config user.email = "dsl-sync-service@test.com"
git add .
git commit -m "Test message"
git push