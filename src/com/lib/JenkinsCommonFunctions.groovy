#!/usr/bin/env groovy
package com.lib

def scheduleBaseJobs(String baseName, String jobName) {
  /* If Job name contains 'base' and branch name is master or develop
  * scheduleBaseJobs schedule the job to run from 1 through 6
  */

  if (baseName.contains('base')  {
    if (jobName == 'master' || jobName == 'develop') {
      println('Condition is working')
    }
  }
}


// '0 1-6 * * *'







return this
