
package com.lib

def runPipeline(){
    properties([parameters([choice(choices: ['1,', '2,', '3,', '4'], description: 'Give me that ', name: 'Choice1')])])
    echo "Hello World"
}