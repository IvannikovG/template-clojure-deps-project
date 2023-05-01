(ns context)

(defonce general-context
  {:zen-resources     ['resources 'handlers 'rpc]

   :config            {:app-env       "dev"}

   :global-migrations [{:name "Create test"
                        :sql "create table if not exists test (id integer, resource jsonb);"
                        :active false
                        :date "2021-09-13"}]

   :migrations        [{:name "Create car"
                        :sql "create table if not exists car (id text, resource jsonb);"
                        :active false
                        :date "2021-09-13"}]})
