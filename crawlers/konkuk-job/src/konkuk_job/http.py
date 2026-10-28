import time
from http.cookiejar import CookieJar
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode, urlsplit, urlunsplit
from urllib.request import HTTPRedirectHandler, HTTPCookieProcessor, Request, build_opener
from urllib.robotparser import RobotFileParser

from .parser import BASE, SourceBlocked, SourceError

USER_AGENT = 'CamNecT-ActivityCrawler/0.1 (+https://github.com/CamNecT/CamNecT-Server)'


class PayloadTooLarge(SourceError):
    def __init__(self, bytes_read=0):
        super().__init__('Source response exceeds size limit')
        self.bytes_read = bytes_read


def source_url(url):
    p = urlsplit(url)
    if p.scheme != 'https' or p.netloc != 'job.konkuk.ac.kr' or p.username or p.password:
        raise SourceError('Fetch outside the HTTPS source host is forbidden')
    return urlunsplit((p.scheme, p.netloc, quote(p.path, safe='/%'), quote(p.query, safe='=&%+'), ''))


class SourceRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return super().redirect_request(req, fp, code, msg, headers, source_url(newurl))


class HttpClient:
    def __init__(self, delay=2.5, timeout=20, opener=None):
        if delay < 2:
            raise ValueError('Minimum source request interval is 2 seconds')
        self.delay, self.timeout, self.last_request = delay, timeout, 0
        self.opener = opener or build_opener(SourceRedirect(), HTTPCookieProcessor(CookieJar()))
        self.robots = None
        self.deadline = None

    def load_robots(self):
        raw, _ = self.fetch(BASE + '/robots.txt', limit=64_000)
        robots = RobotFileParser()
        robots.parse(raw.decode('utf-8-sig').splitlines())
        self.robots = robots

    def fetch(self, url, fields=None, limit=2_000_000):
        url = source_url(url)
        if self.robots and not self.robots.can_fetch(USER_AGENT, url):
            raise SourceError('robots.txt disallows this path')
        for attempt in range(3):
            if self.deadline and time.monotonic() + self.timeout + self.delay >= self.deadline:
                raise SourceBlocked('Request time budget exhausted')
            time.sleep(max(0, self.delay - (time.monotonic() - self.last_request)))
            self.last_request = time.monotonic()
            request = Request(url, data=urlencode(fields).encode() if fields is not None else None,
                              headers={'User-Agent': USER_AGENT, 'Accept-Encoding': 'identity'})
            try:
                with self.opener.open(request, timeout=self.timeout) as response:
                    source_url(response.url)
                    length = response.headers.get('Content-Length', '')
                    if length.isdigit() and int(length) > limit:
                        raise PayloadTooLarge()
                    data = response.read(limit + 1)
                    if len(data) > limit:
                        raise PayloadTooLarge(len(data))
                    return data, response.headers.get_content_type()
            except HTTPError as exc:
                if exc.code in (401, 403, 429):
                    raise SourceBlocked(f'Source denied/rate-limited request ({exc.code})') from exc
                if exc.code < 500 or attempt == 2:
                    raise SourceError(f'Source HTTP error {exc.code}') from exc
            except (URLError, TimeoutError) as exc:
                if attempt == 2:
                    raise SourceError('Source connection failed after retries') from exc
            time.sleep(2 ** (attempt + 1))
        raise SourceError('No response')

    def html(self, url, fields=None):
        raw, mime = self.fetch(url, fields)
        if mime not in ('text/html', 'application/xhtml+xml'):
            raise SourceError('Expected HTML')
        return raw.decode('utf-8-sig'), raw
