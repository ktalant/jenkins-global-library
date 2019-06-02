#!/usr/bin/env groovy

package com.lib


def sendMessage(String type, String status, String channel, String message = null) {
  String color        = ""
  String message      = ""
  String slackUrl     = "https://fuchicorp.slack.com/services/hooks/jenkins-ci/"
  String slackToken   = "slack-token"

  if (!channel.contains("#")) {
    channel = "#" + channel
  }

  switch(status) {
    case "SUCCESS":
      color = "#00FF00"
      if (message != null ) {
        message = """
        Jenkins Job was successfully built.
        email: fuchicorpsolution@gmail.com
        SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})
        """)
      }

      break

    case "FAILURE":
      color = "#FF0000"
      if (message != null ) {
        message = """
        Jenkins build is breaking for some reason. Please go to job and take actions.
        email: fuchicorpsolution@gmail.com
        FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        """
      }
      break

    default:
      color = "#FFFF00"
      if (message != null ) {
        message =  """
        Please add let team know if this is mistake or please send an email
        email: fuchicorpsolution@gmail.com
        STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL}).
        """
      }
      break
  }

  switch(type) {
    case "slack":
      slackSend(channel: channel, color: color, baseUrl: slackUrl, tokenCredentialId: slackToken, message: message)
      break
    default:
      println("No default notification system please use slack")
      break
  }

  println("""
  status:  ${status},
  type:    ${type},
  color:   ${color},
  message: ${message}
  """)
}

return this
