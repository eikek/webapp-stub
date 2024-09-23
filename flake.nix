{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
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
        devshell-tools.packages.${system}.sbt17
        jdk17
        tailwindcss
        terser
      ];
      devshellPkgs =
        ciPkgs
        ++ (with pkgs; [
          jq
          scala-cli
        ]);

      devEnvVars = {
          SBT_OPTS = "-Xmx2G -Xss4m";
          PGPASSWORD = "dev";
          WEBAPPSTUB_POSTGRES_USER = "dev";
          WEBAPPSTUB_POSTGRES_PASSWORD = "dev";
          WEBAPPSTUB_POSTGRES_HOST = "wasdev";
          # 'open' for everyone can signup, 'closed' for no one and
          # 'invite:key' to generate invite keys
          WEBAPPSTUB_SIGNUP_MODE = "open";
          WEBAPPSTUB_AUTH_FIXED = "false"; # use true to remove login
          WEBAPPSTUB_REMEMBER_ME_VALID = "10 days"; # use 0s to disable remember-me
          WEBAPPSTUB_LOGGING_MIN_LEVEL = "Debug";
          WEBAPPSTUB_SERVER_SECRET = "hex:caffee";
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
