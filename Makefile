include .env
export

PAGE_FILE ?= page-logseq-references
PAGE_PATH ?= ~/zettel/pages
OUTPUT_PATH ?= resources/logseqing/
CURLER ?= curl --silent -X POST http://127.0.0.1:12315/api \
                        -H "Authorization: Bearer ${TOKEN}" \
                        -H "Content-Type: application/json"
PAGE_NAME ?= "pull out"

all: run

clean:
	@rm $(OUTPUT_PATH)/*

$(OUTPUT_PATH)%.ndjson: 
	@$(CURLER) -d '{"method": "logseq.Editor.getPageBlocksTree", "args": ["$*"]}' \
		| jq -c '.[] | {content, properties, uuid}' \
		> "$@"

$(PAGE_FILE).ndjson:
	@$(CURLER) -d '{"method": "logseq.db.q", "args": ["(page-property type post)"]}' \
	     | jq -c '.[] | {id, properties, name, uuid, originalName, file}' \
	     > $@

file-names: $(PAGE_FILE).ndjson
	@jq '.name' < $< > $@

grab-pages: file-names
	@xargs -n 1 -I '{}' sh -c "$(MAKE) '$(OUTPUT_PATH){}.ndjson'" < $<

stuff:
	@open https://cjohansen.no/building-static-sites-in-clojure-with-stasis
	@open https://github.com/magnars/stasis

build:
	@lein build-site

run: grab-pages
	@lein ring server

test:
	@lein test

.PHONY: grab-pages grab-page test
