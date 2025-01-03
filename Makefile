include .env
export

PAGE_FILE ?= page-logseq-references
PAGE_PATH ?= ~/zettel/pages
OUTPUT_PATH ?= resources/logseqing/
CURLER ?= curl --silent -X POST http://127.0.0.1:12315/api \
                        -H "Authorization: Bearer ${TOKEN}" \
                        -H "Content-Type: application/json"
PAGE_NAME ?= "pull out"
POST_PATH ?= https://daseinonline.xyz/

all: build

clean:
	@rm $(OUTPUT_PATH)/*

$(OUTPUT_PATH)%.ndjson: 
	@$(CURLER) -d '{"method": "logseq.Editor.getPageBlocksTree", "args": ["$*"]}' \
		| jq -c '.[] | {content, properties, uuid}' \
		> "$@"

$(PAGE_FILE).ndjson:
	@$(CURLER) -d '{"method": "logseq.db.q", "args": ["(page-property type post)"]}' \
		| jq -c '.[]' \
		| grep -v 'ver\":0' \
		| jq -c '{id, properties, name, uuid, originalName, file}' \
		> $@

resources/twtxt: $(PAGE_FILE).ndjson
	@ jq '"\(.properties | .updated // .published) $(POST_PATH)\(.properties.title | gsub("\\s+"; "-"))"' < $< \
		| sed 's/"$$//' \
		| sed 's/^"//' \
		> $@

file-names: $(PAGE_FILE).ndjson
	@jq '.name' < $< > $@

grab-pages: file-names
	@xargs -n 1 -I '{}' sh -c "$(MAKE) '$(OUTPUT_PATH){}.ndjson'" < $<

stuff:
	@open https://cjohansen.no/building-static-sites-in-clojure-with-stasis
	@open https://github.com/magnars/stasis

build:
	@lein run -m stasis-test.core/export $(WHITHER)
	@pushd $(WHITHER) && sudo quark -p 3000 &
	@surf localhost:3000

rebuild-ur-site: build
	@pushd $(WHITHER) && git add . && git commit -m "feat: lol" && git push

run:
	@lein ring server

test:
	@lein test

first-unused-color:
	@bb -i scripts/get-first-unused-color.clj < resources/colours.yaml

.PHONY: grab-pages test run all clean stuff
