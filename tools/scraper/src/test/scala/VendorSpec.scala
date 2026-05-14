import zio.*
import zio.http.URL
import zio.test.*

object VendorSpec extends ZIOSpecDefault:

  private def decode(s: String): URL =
    URL.decode(s).toOption.get

  def spec = suite("Vendor")(
    test("selectVendor returns MeiLeaf for meileaf.com URL") {
      selectVendor(decode("https://meileaf.com/tea/tea-jtic/")).map { v =>
        assertTrue(v == Vendor.MeiLeaf)
      }
    },
    test("selectVendor returns Meetea for store.meetea.cz URL") {
      selectVendor(decode("https://store.meetea.cz/zeleny-caj/heritage-green-2026/")).map { v =>
        assertTrue(v == Vendor.Meetea)
      }
    },
    test("selectVendor fails with UnsupportedVendor for unknown host") {
      selectVendor(decode("https://example.com/foo")).flip.map { err =>
        assertTrue(err.getMessage.contains("example.com"))
      }
    }
  )
