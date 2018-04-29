(ns fk-gen.examples
  (:require
     [honeysql.core :as sql]
     [honeysql.helpers :refer :all :as helpers]
     [honeysql-postgres.format :refer :all]
     [honeysql-postgres.helpers :refer :all]))

(defn generate-sql-insert-stmt-plan
  [table->values table graph]
  (-> (insert-into table)
      (values (table->values table graph))))

(defn get-select-any-stmts
  [table graph]
  (reduce-kv (fn [values pk-table {:keys [fk-column pk-column]}]
               (assoc values fk-column {:select [pk-column] :from [pk-table] :limit 1}))
             {} (graph table)))

(defn mock-table->values
  [table graph]
  [(merge {:fk-column "fk-val"} (get-select-any-stmts table graph))])

