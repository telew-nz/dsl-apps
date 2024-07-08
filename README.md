# Telew Apps

This contains a copy of all the source code for all the Telew apps.

## DSL Sync Server

You can also run the [script][DslSyncServer] that connects with backend instances of Telew apps and provides syncing functionallity with this GitHub repo to the platform.

## Build

For building the app use provided [script][BuildScript]. You'll need to update [configuration][application.conf] file with server url, DB and git ssh credentials.

## Manual updates

The source code can be updated imported or exported manually using provided scripts. No pre-made scripts for that as we expect you to run it through DSL sync server, so launch those manually.

[DslSyncServer]: ./src/main/scala/com/dap/DslSyncServer.scala
[BuildScript]: ./build.sh
[application.conf]: ./conf/application.conf
