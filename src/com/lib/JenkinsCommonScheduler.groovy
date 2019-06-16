#!/usr/bin/env groovy
package com.lib

def runBaseJobs(name) {
  if (name.contains('base')  {
    properties([[$class: 'RebuildSettings',
    autoRebuild: false,
    rebuildDisabled: false],
    pipelineTriggers([cron('0 1-6 * * *')])])
  }
}






return this
