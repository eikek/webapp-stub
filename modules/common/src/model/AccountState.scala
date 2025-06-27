package webappstub.common.model

enum AccountState:
  case Active
  case Inactive

  lazy val name: String = productPrefix.toLowerCase

object AccountState:
  def fromString(s: String): Either[String, AccountState] =
    AccountState.values
      .find(_.name.equalsIgnoreCase(s))
      .toRight(s"Invalid account state: $s")
