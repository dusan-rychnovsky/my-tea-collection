import zio.*
import zio.http.*

final class UnsupportedVendor(host: String)
  extends RuntimeException(s"no vendor registered for host: $host")

enum Vendor(
    val displayName: String,
    val host: String,
    parse: (String, URL) => IO[ParseError, TeaInfo]
):
  case MeiLeaf extends Vendor("Mei Leaf", "meileaf.com", parseMeileafTea)
  case Meetea  extends Vendor("Meetea", "store.meetea.cz", parseMeeteaTea)

  def scrape(url: URL): ZIO[Client, Throwable, TeaInfo] =
    fetch(url.encode).flatMap(parse(_, url))

def selectVendor(url: URL): IO[UnsupportedVendor, Vendor] =
  ZIO
    .fromOption(Vendor.values.find(v => url.host.contains(v.host)))
    .orElseFail(UnsupportedVendor(url.host.getOrElse("<unknown>")))
