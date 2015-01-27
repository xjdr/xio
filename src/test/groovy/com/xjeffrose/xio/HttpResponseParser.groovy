
import com.xjeffrose.xio.*
import spock.lang.*
import java.nio.*

class HttpResponseParserSpock extends Specification {
  def payload = """\
    HTTP/1.1 200 OK
    Server: nginx
    Date: Tue, 27 Jan 2015 04:20:39 GMT
    Content-Type: text/plain; charset=UTF-8
    Content-Length: 13
    Connection: close
    X-RTFM: Learn about this site at http://bit.ly/icanhazip-faq and don't abuse the service
    X-BECOME-A-RACKER: If you're reading this, apply here: http://rackertalent.com/
    Access-Control-Allow-Origin: *
    Access-Control-Allow-Methods: GET

    73.208.4.198
    """.stripIndent().replaceAll("\n", "\r\n")

  def "HttpResponseParser parses headers correctly"() {
    when:
    def buffer = ByteBuffer.allocateDirect(1024)
    def response = new HttpResponse()
    def parser = new HttpResponseParser(response)
    buffer.put(ByteBuffer.wrap(payload.getBytes()))
    def parsedOk = parser.parse(buffer)

    then:
    parsedOk == true
    response.httpVersion() == "HTTP/1.1"
    response.headers.headerMap.size() == 9
    response.headers.get("Server") == "nginx"
    response.headers.get("Date") == "Tue, 27 Jan 2015 04:20:39 GMT"
    response.headers.get("Content-Type") == "text/plain; charset=UTF-8"
    response.headers.get("Content-Length") == "13"
    response.headers.get("Connection") == "close"
    response.headers.get("X-RTFM") == "Learn about this site at http://bit.ly/icanhazip-faq and don't abuse the service"
    response.headers.get("X-BECOME-A-RACKER") == "If you're reading this, apply here: http://rackertalent.com/"
    response.headers.get("Access-Control-Allow-Origin") == "*"
    response.headers.get("Access-Control-Allow-Methods") == "GET"
  }
}
