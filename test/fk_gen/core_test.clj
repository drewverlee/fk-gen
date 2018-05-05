(ns fk-gen.core-test
  (:require [clojure.test :refer :all]
            [fk-gen.core :as fk-gen]
            [fk-gen.table-graph-to-sql :refer [->insert ->select-any]]
            [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s])
  (:import [com.opentable.db.postgres.embedded EmbeddedPostgres]))

(defn with-postgres
  [f]
  (let [db (-> (EmbeddedPostgres/builder)
               (.setPort 3001)
               .start)
        ]
    (try
      (f)
      (finally
        (.close db)))))

(use-fixtures :once with-postgres)

;; TODO this should be moved into the app
(deftest test-gen
  (testing "that given a table we can create a list of sql insert statements for it and all its dependencies"
    (let [db-info {:classname "org.postgresql.Driver"
                   :subprotocol "postgresql"
                   :subname "//localhost:3001/postgres"
                   :schema "public"
                   :password "postgres"
                   :user "postgres"
                   :sslfactory "org.postgresql.ssl.NonValidatingFactory"}

          create-persons-table "CREATE TABLE persons (id serial primary key,
                                                      name text);"

          create-dogs-table "CREATE TABLE dogs (id serial primary key,
                                                name text,
                                                owner integer references persons(id));"
          mock-table->values (fn [table graph] [(merge {:id 1 :name (str (name table) "-name")} (->select-any table graph))])
          table-graph->insert-stmt-plan (partial ->insert mock-table->values)]
      (jdbc/execute! db-info create-persons-table)
      (jdbc/execute! db-info create-dogs-table)
      ;; create foreign key deps for the dogs table and insert them into the db.
      (->> (fk-gen/generate {:table :dogs :db-info db-info :table-graph->insert-stmt-plan table-graph->insert-stmt-plan})
           flatten
           (map #(sql/format %))
           (run! #(jdbc/execute! db-info %)))
      (is (= {:dog_name "dogs-name" :person_name "persons-name"}
             (jdbc/query db-info ["SELECT d.name as dog_name, p.name as person_name FROM dogs d JOIN persons p ON p.id = d.owner"] {:result-set-fn first}))
          "the sql insert statements generated weren't valid"))))
