package webappstub.common.model

final case class Contact(
    id: ContactId,
    owner: AccountId,
    name: Name,
    email: Option[Email],
    phone: Option[PhoneNumber]
):
  val fullName = name.fullName
  def contains(s: String): Boolean =
    name.contains(s) ||
      email.exists(_.contains(s)) ||
      phone.exists(_.contains(s))

  def withoutId: NewContact = NewContact(owner, name, email, phone)
