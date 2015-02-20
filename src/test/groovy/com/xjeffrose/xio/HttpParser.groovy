import com.xjeffrose.xio.*
import spock.lang.*
import java.nio.*

class HttpParserSpock extends Specification {
  def payload = """\
    GET / HTTP/1.1
    User-Agent: curl/7.35.0
    Host: localhost:8000
    Accept: */*

    """.stripIndent().replaceAll("\n", "\r\n")

  def "HttpParser parses headers correctly"() {
    when:
    def parser = new HttpParser()
    def request = new HttpRequest()
    request.buffer.put(ByteBuffer.wrap(payload.getBytes()))
    def parsedOk = parser.parse(request)

    then:
    parsedOk == true
    request.method() == "GET"
    request.uri() == "/"
    request.httpVersion() == "HTTP/1.1"
    request.headers.headerMap.size() == 3
    request.headers.get("User-Agent") == "curl/7.35.0"
    request.headers.get("Host") == "localhost:8000"
    request.headers.get("Accept") == "*/*"
  }
}
