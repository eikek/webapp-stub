package webappstub.server.data

enum UiTheme:
  case Light
  case Dark

  lazy val name: String = productPrefix.toLowerCase()

  def fold[A](ifLight: => A, ifDark: => A): A = this match
    case Light => ifLight
    case Dark  => ifDark

object UiTheme:
  def fromString(str: String): Either[String, UiTheme] =
    UiTheme.values
      .find(_.name.equalsIgnoreCase(str))
      .toRight(s"Invalid ui theme: $str (use 'light' or 'dark')")

  def cycle(current: UiTheme): UiTheme =
    current match
      case Light => Dark
      case Dark  => Light
