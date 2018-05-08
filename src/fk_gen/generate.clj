;; ### Generators
;; These functions generate SQL insert statements in honeySQL format, regular SQL and also just insert the generated data for you.
;; This namespace brings together all the functionality and is here users of library should go for public facing functions.
(ns fk-gen.generate
  (:require [fk-gen.core :refer [get-fk-deps fk-deps->sql-plan]]
            [clojure.spec.alpha :as s]
            [table-spec.core :as t]
            [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [fk-gen.table-graph-to-sql :refer [->select-any ->insert]]
            [com.rpl.specter :refer [transform MAP-KEYS ALL]]
            [clojure.spec.gen.alpha :as gen]))

(defn- ->sql-plan
  "Returns a vector of sql insert statement (honeysql format) necessary to fulfill all the foreign key constraints of the given table
  `table`                         :keyword : the name of the table
  `db-info`                       :hashmap : a map describing the database connection information see https://github.com/clojure/java.jdbc.
  `table-graph->insert-stmt-plan` :fn      : a two arity fn [table,graph] that returns a vector of honeysql formatted insert statements"
  [{:keys [db-info table table-graph->insert-stmt-plan]}]
  (fk-deps->sql-plan {:table table
                      :table-graph->insert-stmt-plan table-graph->insert-stmt-plan
                      :fk-deps (get-fk-deps db-info)}))

(defn ->sql
  "Returns a collection of sql insert Statements that full fill all the foreign key dependencies of the table
  `table`   :keyword : the name of the table
  `db-info` :hashmap : a map describing the database connection information see https://github.com/clojure/java.jdbc."
  [{:keys [db-info table] :as db-args}]
  (let [db-info->connection-uri (fn [{:keys [subprotocol subname user schema password]}]
                                  {:connection-uri (str "jdbc:" subprotocol ":" 
                                                        subname "?user=" 
                                                        user "&password=" password)
                                   :schema schema})
        gen-values (fn [table]
                     (transform [MAP-KEYS] #(keyword (name %))
                                (last (gen/sample (s/gen (keyword (str "table/" (name table))) 30)))))
        table-graph->insert-stmt-plan (partial ->insert (fn [table graph]
                                                    [(merge (gen-values table) (->select-any table graph))]))]
    (do (-> (db-info->connection-uri db-info)
            (t/tables)
            (t/register))
        (map sql/format
             (flatten (->sql-plan (assoc db-args :table-graph->insert-stmt-plan table-graph->insert-stmt-plan)))))))

(defn ->sql-and-insert!
  "Inserts sql insert statements that full fill all the foreign key dependencies of the table
  `table`   :keyword : the name of the table
  `db-info` :hashmap : a map describing the database connection information see https://github.com/clojure/java.jdbc."
  [{:keys [db-info table] :as db-args}]
  (->> (->sql db-args)
       (run! #(jdbc/execute! db-info %))))
