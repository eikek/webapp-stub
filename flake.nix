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
          WEBAPPSTUB_SIGNUP_MODE = "invite:secret42";
          WEBAPPSTUB_AUTH_FIXED = "false"; # use true to remove login
          WEBAPPSTUB_REMEMBER_ME_VALID = "0s"; # use 0s to disable remember-me
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
            }
          ];
        };
      };
    };
}
