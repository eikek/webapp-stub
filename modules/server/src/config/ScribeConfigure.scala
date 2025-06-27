package webappstub.server.config

import cats.effect.Sync

import scribe.Level
import scribe.format.Formatter
import scribe.jul.JULHandler
import scribe.writer.SystemOutWriter

object ScribeConfigure {
  def configure[F[_]: Sync](cfg: LogConfig): F[Unit] =
    Sync[F].delay {
      replaceJUL()
      unsafeConfigure(cfg)
    }

  def unsafeConfigure(cfg: LogConfig): Unit = {
    unsafeConfigure(scribe.Logger.root, cfg.format, cfg.minimumLevel)
    cfg.extraLevel.foreach { case (name, level) =>
      unsafeConfigure(scribe.Logger(name), cfg.format, level)
    }
  }
  def unsafeConfigure(logger: String, cfg: LogConfig): Unit = {
    val log = scribe.Logger(logger)
    val level = cfg.extraLevel.getOrElse(logger, cfg.minimumLevel)
    unsafeConfigure(log, cfg.format, level)
  }

  def unsafeConfigure(
      logger: scribe.Logger,
      format: LogConfig.Format,
      level: Level
  ): Unit = {
    val mods: List[scribe.Logger => scribe.Logger] = List(
      _.clearHandlers(),
      _.withMinimumLevel(level),
      l =>
        if (logger.id == scribe.Logger.RootId) {
          format match {
            case LogConfig.Format.Fancy =>
              l.withHandler(formatter = Formatter.enhanced, writer = SystemOutWriter)
            case LogConfig.Format.Plain =>
              l.withHandler(formatter = Formatter.classic, writer = SystemOutWriter)
          }
        } else l,
      _.replace()
    )

    mods.foldLeft(logger)((l, mod) => mod(l))
    ()
  }

  def replaceJUL(): Unit = {
    scribe.Logger.system // just to load effects in Logger singleton
    val julRoot = java.util.logging.LogManager.getLogManager.getLogger("")
    julRoot.getHandlers.foreach(julRoot.removeHandler)
    julRoot.addHandler(JULHandler)
  }
}
