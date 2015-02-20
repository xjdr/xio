import com.xjeffrose.xio.*;

class RestService extends Service {
  def owners = [:]
  def void handleGet() {
    def owner = route.groups(req.uri()).get("owner")
    def hi_fives = (owners[owner] == null) ? owners[owner] = 0 : owners[owner]
    println owners
    resp.ok()
    resp.body("{'hi_fives': " + hi_fives + "}\n")
  }

  def void handlePost() {
    def key = "hi_fives="
    def body = req.body.toString()
    def idx = body.indexOf(key)
    def payload = body.substring(idx+key.length()).toInteger()
    def owner = route.groups(req.uri()).get("owner")
    def hi_fives = owners[owner] = payload
    println owners
    resp.ok()
    resp.body("{'hi_fives': " + hi_fives + "}\n")
  }

  def void handlePut() {
    def owner = route.groups(req.uri()).get("owner")
    def hi_fives = (owners[owner] == null) ? owners[owner] = 1 : ++(owners[owner])
    println owners
    resp.ok()
    resp.body("{'hi_fives': " + hi_fives + "}\n")
  }

  def void handleDelete() {
    def owner = route.groups(req.uri()).get("owner")
    def hi_fives = owners[owner] = 0
    println owners
    resp.ok()
    resp.body("{'hi_fives': " + hi_fives + "}\n")
  }
}

rootService = new RestService()
s = new Server()
//s.addRoute("/", rootService)
s.addRoute("/hands/:owner/slap", rootService)

sf = s.serve(8080)
println("xio from groovy")
