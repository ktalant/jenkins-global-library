import groovy.json.JsonSlurperClassic


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}


def showUserInfor(jsonData) {
  def data = jsonParse(readFile(jsonData))
  data.each() {
    println("Users first name :${it['first_name']}, Last name :${it['last_name']}")
  }
}
