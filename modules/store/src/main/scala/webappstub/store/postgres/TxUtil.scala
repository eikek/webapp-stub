package webappstub.store.postgres

import cats.effect.*

import skunk.Session

private trait TxUtil:

  extension [F[_]: Sync](session: Resource[F, Session[F]])
    def inTx[A](f: Session[F] => F[A]) =
      session.use(s => s.transaction.use(_ => f(s)))
