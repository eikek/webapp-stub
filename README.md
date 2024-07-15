# webapp stub

This is a template repository for making starting a new Scala3 project
a bit quicker. It provides a runnable web application, github actions
etc.

The idea is to clone it, rename and modify as needed.

Please note, this is very opinionated and intended for myself when
starting some small projects. :smile:

## Outline

It consists of these modules:

- `common` contains datastructures and utility code which can be used
  across all other modules, it should be light on dependencies
- `store` contains the code accessing the postgresql database using
  `skunk`
- `backend` uses `store` to create the overall application library
- `server` provides the web application, based on
  [http4s](https://http4s.org), [htmx](https://htmx.org) and
  [tailwindcss](https://tailwindcss.com/). Configuration is done using
  [ciris](https://cir.is/)

The application is a tiny "manage contacts" example application as
described in the [htmx book](https://hypermedia.systems/).

The favicon example is from [favicon.io](https://favicon.io).

### Included "Stub-Features"

It is usually a bit easier to remove things, so this stub implements
some basic functiality that is useful across all projcts.

- basic sbt setup with the above modules, making use of the following:
  - typelevel stack: cats-effect, fs2
  - postgresql and skunk
  - http4s, htmx, htmx4s and scalatags
  - ciris for reading configuration
  - borer for json
  - scribe for logging
- dark/light ui theme with tailwind, controlled via a cookie
- basic site layout with a top bar
- user authenication with password or "auto-user" mode (then some
  pre-defined user is logged in automatically)
- user registration
- a "version" route for getting version information
- a http4s `ContextMiddleware` that is supposed to handle common
  request inputs, like authentication and settings (ui theme) and
  possibly other things like language etc.
- github actions for doing ci and release zips to github release page
- release-drafter setup
- nix dev and ci setup

### Included Code Examples

As I tend to forget details when not using it regularily, it is nice
to come to some examples to refresh memory. Here are some included
details to serve this purpose:

- Using htmx `hx-on` attribute to change css classes when hovering an element
- Example skunk codecs for (nested) case classes
- a simple idea for i18n with scalatags and htmx

## Nix

The `flake.nix` provides a convenient development setup. It makes sure
sbt and other tools are available. It also provides a development
container (or vm), that makes external services available, like the
postgresql database. When running on NixOS, the container can be used.
If not running NixOS, it a vm can be used instead, just enter a
different development shell with `nix develop .#vm`. This is provided
by [devshell-tools](https://github.com/eikek/devshell-tools).

The default development shell can be entered via `nix develop`.

There is a another development shell, `ci`, which is used by the `ci`
github action.


## Usage

Clone this repo and then use the `init.sh` script to rename to the new
project name. It also removes `.git` folder and creates it anew.
