# webapp stub

This is a template repository for making starting a new Scala3 project
a bit quicker. It provides a runnable web application, github actions
etc.

The idea is to clone it, rename and modify as needed.

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

## Nix

The `flake.nix` provides a convenient development setup. It makes sure
sbt and other tools are available. It also provides a development
container when running on NixOS, that makes external services
available, like the postgresql database. If not running NixOS, it a vm
can be used instead, just enter a different development shell with
`nix develop .#vm`. This is provided by
[devshell-tools](https://github.com/eikek/devshell-tools).

The default development shell can be entered via `nix develop`.

There is a another development shell, `ci`, which is used by the `ci`
github action.


## Usage

Clone this repo and then use the `init.sh` script to rename to the new
project name. It also removes `.git` folder and creates it anew.
