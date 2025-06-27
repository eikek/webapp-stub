package webappstub.store.postgres

import java.time.Instant

import webappstub.common.model.InviteKey

final private case class InviteRecord(id: Long, key: InviteKey, created: Instant)
