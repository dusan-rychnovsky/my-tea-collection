import zio.*
import zio.http.*

final class UnsupportedVendor(host: String)
  extends RuntimeException(s"no vendor registered for host: $host")

case class Vendor(
  name:   String,
  host:   String,
  scrape: URL => ZIO[Client, Throwable, TeaInfo]
)

val vendors: List[Vendor] = List(meileafVendor, meeteaVendor)

def selectVendor(url: URL): IO[UnsupportedVendor, Vendor] =
  ZIO
    .fromOption(vendors.find(v => url.host.contains(v.host)))
    .orElseFail(UnsupportedVendor(url.host.getOrElse("<unknown>")))
