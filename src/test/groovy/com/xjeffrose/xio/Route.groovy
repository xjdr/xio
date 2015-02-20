import com.xjeffrose.xio.*
import spock.lang.*

class RouteSpock extends Specification {
  def "Parses route correctly"() {
    expect:
    Route.build(a).pathPattern.toString() == b

    where:
    a                                      | b
    "/"                                    | "/[/]?"
    "/api/people/:person"                  | "/api/people/(?<person>[^/]*)[/]?"
    "/api/people/:person/hands/:hand/slap" | "/api/people/(?<person>[^/]*)/hands/(?<hand>[^/]*)/slap[/]?"
  }

  def "Returns groups correctly"() {
    expect:
    Route.build(a).groups(b) == c

    where:
    a                                      | b                                  | c
    "/"                                    | "/"                                | [:]
    "/api/people/:person"                  | "/api/people/jeff"                 | [person:"jeff"]
    "/api/people/:person"                  | "/api/people/jeff/"                | [person:"jeff"]
    "/api/people/:person/hands/:hand/slap" | "/api/people/jeff/hands/left/slap" | [person:"jeff", hand:"left"]
  }
}
