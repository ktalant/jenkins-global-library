import groovy.json.JsonSlurperClassic


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parse(json)
}


def showUserInfor(jsonData) {
  def data = jsonParse(readFile(jsonData))
  println(data)
  // data.each() {
  //   println("Users first name :${it['first_name']}, Last name :${it['last_name']}")
  // }
}
