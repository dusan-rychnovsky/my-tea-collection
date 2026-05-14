import zio.*
import zio.http.*

final class UnsupportedVendor(host: String)
  extends RuntimeException(s"no vendor registered for host: $host")

enum Vendor(val displayName: String, val host: String):
  case MeiLeaf extends Vendor("Mei Leaf", "meileaf.com")
  case Meetea  extends Vendor("Meetea", "store.meetea.cz")

  def scrape(url: URL): ZIO[Client, Throwable, TeaInfo] = this match
    case Vendor.MeiLeaf => fetch(url.encode).flatMap(parseMeileafTea(_, url))
    case Vendor.Meetea  => fetch(url.encode).flatMap(parseMeeteaTea(_, url))

def selectVendor(url: URL): IO[UnsupportedVendor, Vendor] =
  ZIO
    .fromOption(Vendor.values.find(v => url.host.contains(v.host)))
    .orElseFail(UnsupportedVendor(url.host.getOrElse("<unknown>")))
