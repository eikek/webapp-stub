package webappstub.common.model

final case class NewContact(name: Name, email: Option[Email], phone: Option[PhoneNumber]):
  val fullName = name.fullName
  def withId(id: ContactId): Contact = Contact(id, name, email, phone)
