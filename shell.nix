{ pkgs ? import <nixpkgs> { } }:
pkgs.mkShell {
	name = "dasein-online";
	buildInputs = with pkgs; [
		clojure
		gnumake
		leiningen
		quark
	];
	shellHook = ''
		echo "welcome to hell"
	'';
}
