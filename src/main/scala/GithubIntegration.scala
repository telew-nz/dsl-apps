package scala

import sys.process._

object GithubIntegration {

    def prepareGitUser: Unit = 
        Seq("bash", "-c", """
            |git config --unset-all user.name
            |git config --unset-all user.email
            |git config user.name "$GITHUB_USER_NAME"
            |git config user.email "$GITHUB_USER_EMAIL"
        """.stripMargin).!!

    def pushChanges: Unit =
        Seq("bash", "-c", s"""
            |eval "$$(ssh-agent -s)"
            |ssh-add "$$GITHUB_SSH_PATH"
            |git add .
            |git commit -m "[${java.time.LocalDateTime.now()}] Updated all apps"
            |git push
        """.stripMargin).!!

    def pullChanges: Unit =
        Seq("bash", "-c", s"""
            |eval "$$(ssh-agent -s)"
            |ssh-add "$$GITHUB_SSH_PATH"
            |git pull
        """.stripMargin).!!

}