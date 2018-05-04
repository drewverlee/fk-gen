(ns fk-gen.table-graph-to-sql
  "These functions serve mostly as an example of how to construct a
  `table-graph->insert-stmt-plan` function which fulfills its contract of taking a `graph` and `table` and
  returning a vector of honeysql insert statements "
  (:require
     [honeysql.core :as sql]
     [honeysql.helpers :refer :all :as helpers]
     [honeysql-postgres.format :refer :all]
     [honeysql-postgres.helpers :refer :all]))


(defn ->select-any
  [table graph]
  (reduce-kv (fn [values pk-table {:keys [fk-column pk-column]}]
               (assoc values fk-column {:select [pk-column] :from [pk-table] :limit 1}))
             {} (graph table)))

(defn ->insert
  [table->values table graph]
  (-> (insert-into table)
      (values (table->values table graph))))
