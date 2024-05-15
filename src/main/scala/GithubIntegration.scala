package scala

import sys.process._

object GithubIntegration {

    def pushChanges: Unit = {
        "./commitAndPushChanges.sh".!!
    }

}