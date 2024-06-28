package com.dap

import scala.sys.process._

object GithubIntegration {

    def prepareGitUser(): Unit =
        Seq("bash", "-c", s"""
            |git config --unset-all user.name
            |git config --unset-all user.email
            |git config user.name ${AppConfig.config.github.userName}
            |git config user.email ${AppConfig.config.github.userEmail}
        """.stripMargin).!!

    def pushChanges(): Unit =
        Seq("bash", "-c", s"""
            |eval "$$(ssh-agent -s)"
            |ssh-add ${AppConfig.config.github.sshPath}
            |git add .
            |git commit -m "[${java.time.LocalDateTime.now()}] Updated all apps"
            |git push
        """.stripMargin).!!

    def pullChanges(): Unit =
        Seq("bash", "-c", s"""
            |eval "$$(ssh-agent -s)"
            |ssh-add ${AppConfig.config.github.sshPath}
            |git pull
        """.stripMargin).!!

}