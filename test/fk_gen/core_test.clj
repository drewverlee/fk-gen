(ns fk-gen.core-test
  (:require [clojure.test :refer :all]
            [fk-gen.core :as fk-gen]
            [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc])
  (:import [com.opentable.db.postgres.embedded EmbeddedPostgres]))

(defn with-postgres [f]
  (let [db (-> (EmbeddedPostgres/builder)
               (.setPort 3001)
               .start)
        ]
    (try
      (f)
      (finally
        (.close db)))))

(use-fixtures :once with-postgres)

(deftest test-create
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
                                                owner integer references persons(id));"]
      (jdbc/execute! db-info create-persons-table)
      (Thread/sleep 20000)
      (jdbc/execute! db-info create-dogs-table)
      (Thread/sleep 20000)
      ;; create foreign key deps for the dogs table and insert them into the db.
      (->> (fk-gen/create :dogs db-info)
           flatten
           (map #(sql/format %))
           (map #(jdbc/execute! db-info %)))
      (Thread/sleep 20000)
      (is (= 1
             (jdbc/query db-info ["SELECT COUNT(*) FROM dogs"] {:row-fn :count :result-set-fn first}))
          "the sql insert statements created weren't valid"))))

