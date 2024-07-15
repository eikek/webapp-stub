package webappstub.store.migration

package object changesets {
  val all: Seq[ChangeSet] = List(
    Account.get,
    Account.invitation,
    Contact.get
  )
}
