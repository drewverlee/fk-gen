(ns fk-gen.core-test
  (:require [clojure.test :refer :all]
            [fk-gen.generate :as generate]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s])
  (:import [com.opentable.db.postgres.embedded EmbeddedPostgres]))

(defn with-postgres
  [f]
  (let [db (-> (EmbeddedPostgres/builder)
               (.setPort 3001)
               .start)]
    (try
      (f)
      (finally
        (.close db)))))

(use-fixtures :once with-postgres)

;; Here we just check that we can return the correct rows. No need to check the values, so we do a count.
(deftest test-generate-sql-and-insert!
  (testing "that given a table we can insert all it and all of its foreign key dependencies."
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
                                                owner integer references persons(id));"]
      (jdbc/execute! db-info create-persons-table)
      (jdbc/execute! db-info create-dogs-table)
      (->> (generate/->sql-and-insert! {:table :dogs :db-info db-info})
           (run! #(jdbc/execute! db-info %)))
      (is (= 1
             (jdbc/query db-info ["SELECT COUNT(*) FROM dogs d JOIN persons p ON p.id = d.owner"] {:result-set-fn first :row-fn :count})))
          "the sql insert statements generated weren't valid")))
