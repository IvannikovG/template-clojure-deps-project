DEVELOPMENT=development
PGUSER = postgres
PGPASSWORD = postgres
PGPORT = 6434
PGTESTPORT = 6435
PGDATABASE = general_db
JWT_SECRET = PUSHATHECATSECRET
RELEASE = "local"
.EXPORT_ALL_VARIABLES:
.PHONY: test build

down:
	docker-compose down --remove-orphans

up: down
	docker-compose up -d --remove-orphans db db-test

repl:
	export PGHOST="localhost" && clj -A:clj:nrepl

npm:
	npm install

remove-jars:
	rm -rf target/

docker-run:
	docker run -it -e PGHOST='host.docker.internal' -e JWT_SECRET='PUSHATHECATSECRET' -e PGPORT='5434' -p 7002:7002 -e PGPASSWORD='postgres' <template>

tst:
	clojure -A:test:kaocha

test!: up
	clojure -A:test:kaocha

shadow:
	clojure -M:cljs:ui:nrepl:shadow

