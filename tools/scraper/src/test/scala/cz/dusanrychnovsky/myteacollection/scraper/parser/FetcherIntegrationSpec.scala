package cz.dusanrychnovsky.myteacollection.scraper.parser

import zio.*
import zio.http.*
import zio.test.*
import zio.test.TestAspect.*

object FetcherIntegrationSpec extends ZIOSpecDefault:
  def spec = suite("Fetcher integration")(
    test("downloads Google's homepage") {
      for
        html <- fetch("http://www.google.com")
      yield assertTrue(html.contains("Google"))
    }
  ).provide(Client.default) @@ withLiveClock @@ tag("integration")
