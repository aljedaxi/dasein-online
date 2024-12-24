#! /usr/bin/env nix-shell
#! nix-shell -i python3 -p python3 python312Packages.rdflib

import rdflib
g = rdflib.Graph()

result = g.parse('./resources/specs.ttl', format='ttl')
print(result)

g.query("""
SELECT * WHERE {
  ?s ?p ?o .
}
""")

for stmt in g:
    print(stmt)
