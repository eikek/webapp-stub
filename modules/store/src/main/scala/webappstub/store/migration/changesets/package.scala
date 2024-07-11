package webappstub.store.migration

package object changesets {
  val all: Seq[ChangeSet] = List(
    Contact.get,
  )
}
