package webappstub.server.data

enum UiLanguage(val iso3: String, val iso2: String, val label: String):
  case English extends UiLanguage("eng", "gb", "English")
  case German extends UiLanguage("ger", "de", "Deutsch")
//  case French extends UiLanguage("fra", "fr", "Français")

object UiLanguage:
  val all: List[UiLanguage] = UiLanguage.values.toList

  private val allStr = UiLanguage.values.map(_.iso3).mkString(", ")

  def fromString(str: String): Either[String, UiLanguage] =
    UiLanguage.values
      .find(l => l.iso3.equalsIgnoreCase(str) || l.iso2.equalsIgnoreCase(str))
      .toRight(s"Invalid language: $str. Available: $allStr")
