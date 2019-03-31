#!/usr/bin/env groovy
package com.lib


def branchName = "${JOB_BASE_NAME}"




def runPipeline(String branchName) {
  println("${branchName}")
}
