import com.xjeffrose.xio.*
import spock.lang.*
import java.nio.*


class HttpParser extends Specification {
def payload = """\
GET / HTTP/1.1\r
User-Agent: curl/7.35.0\r
Host: localhost:8000\r
Accept: */*\r\n\r\n"""

    def "HttpParser parses headers correctly"() {
        when:
        def buffer = ByteBuffer.allocateDirect(1024)
        def parser = new HttpParser()
        buffer.put(ByteBuffer.wrap(payload.getBytes()))
        def parsedOk = parser.parse(buffer)
        def request = parser.request()

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
