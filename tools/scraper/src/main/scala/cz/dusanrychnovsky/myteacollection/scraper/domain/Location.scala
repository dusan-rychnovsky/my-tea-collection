package cz.dusanrychnovsky.myteacollection.scraper.domain

opaque type Location = ::[String]

object Location:
  def apply(head: String, more: String*): Location =
    new ::(head, more.toList)

  def fromList(parts: List[String]): Option[Location] = parts match
    case Nil    => None
    case h :: t => Some(new ::(h, t))

  extension (loc: Location) def value: ::[String] = loc
