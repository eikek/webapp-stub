{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    devshell-tools.url = "github:eikek/devshell-tools";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    devshell-tools,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
      ciPkgs = with pkgs; [
        devshell-tools.packages.${system}.mill21
        devshell-tools.packages.${system}.postgres-fg
        jdk21
        tailwindcss_4
        terser
      ];
      devshellPkgs =
        ciPkgs
        ++ (with pkgs; [
          jq
          scala-cli
          mermaid-cli
          metals
        ]);

      devEnvVars = {
          SBT_OPTS = "-Xmx2G -Xss4m";
          PGPASSWORD = "dev";
          WEBAPPSTUB_POSTGRES_USER = "dev";
          WEBAPPSTUB_POSTGRES_PASSWORD = "dev";
          WEBAPPSTUB_POSTGRES_HOST = "wasdev";
          WEBAPPSTUB_POSTGRES_PORT = "5432";
          # 'open' for everyone can signup, 'closed' for no one and
          # 'invite:key' to generate invite keys
          WEBAPPSTUB_SIGNUP_MODE = "open";

          WEBAPPSTUB_AUTH_INTERNAL_ENABLED = "true";
          WEBAPPSTUB_AUTH_INTERNAL_REMEMBER_ME_VALID = "10 days"; # use 0s to disable remember-me
          WEBAPPSTUB_LOGGING_MIN_LEVEL = "Debug";
          WEBAPPSTUB_SERVER_SECRET = "hex:caffee";

          #WEBAPPSTUB_AUTH_OPENID_PROVIDERS = "keycloak";
          #WEBAPPSTUB_OPENID_KEYCLOAK_PROVIDER_URI = "http://wasdev:8180/realms/Webappstub";
          #WEBAPPSTUB_OPENID_KEYCLOAK_CLIENT_ID = "webappstub";
          #WEBAPPSTUB_OPENID_KEYCLOAK_CLIENT_SECRET = "<the-secret>";
      };
    in {
      formatter = pkgs.alejandra;

      devShells = {
        default = pkgs.mkShellNoCC (devEnvVars // {
          buildInputs = (builtins.attrValues devshell-tools.legacyPackages.${system}.cnt-scripts) ++ devshellPkgs;
          DEV_CONTAINER = "wasdev";
        });
        vm = pkgs.mkShellNoCC (devEnvVars // {
          buildInputs = (builtins.attrValues devshell-tools.legacyPackages.${system}.vm-scripts) ++ devshellPkgs;
          DEV_VM = "wasvm";
        });
        ci = pkgs.mkShellNoCC {
          buildInputs = ciPkgs;
          SBT_OPTS = "-Xmx2G -Xss4m";
        };
      };
    })
    // {
      nixosConfigurations = {
        wasdev = devshell-tools.lib.mkContainer {
          system = "x86_64-linux";
          modules = [
            {
              services.dev-postgres = {
                enable = true;
                databases = ["webappstub"];
              };
              services.dev-keycloak = {
                enable = true;
              };
              services.dev-authentik = {
                enable = true;
              };
              networking.hostName = "wasdev";
            }
          ];
        };
        wasvm = devshell-tools.lib.mkVm {
          system = "x86_64-linux";
          modules = [
            {
              services.dev-postgres = {
                enable = true;
                databases = ["webappstub"];
              };
              services.dev-keycloak = {
                enable = true;
              };
              services.dev-authentik = {
                enable = true;
              };
            }
          ];
        };
      };
    };
}
