package webappstub.backend.auth

enum SessionInfo:
  case SessionOnly(token: String)
  case RememberMe(token: String)
  case Session(session: String, rememberMe: String)

  def sessionToken: Option[String] = this match
    case SessionOnly(token)  => Some(token)
    case Session(session, _) => Some(session)
    case RememberMe(_)       => None

  def rememberMeToken: Option[String] = this match
    case SessionOnly(_)    => None
    case Session(_, token) => Some(token)
    case RememberMe(token) => Some(token)
