(ns fk-gen.table-graph-to-sql
  "These functions can be used to build a
  `table-graph->insert-stmt-plan` function which fulfills its contract of taking a `graph` and `table` and
  returning a vector of honeySQL insert statements. Check the core_test for an example of how this is done."
  (:require [honeysql.helpers :refer [insert-into values]]))

(defn ->select-any
  "Returns a vector of select any sql statements"
  [table graph]
  (reduce-kv (fn [values pk-table {:keys [fk-column pk-column]}]
               (assoc values fk-column {:select [pk-column] :from [pk-table] :limit 1}))
             {} (graph table)))

(defn ->insert
  "returns a vector of insert sql statements"
  [table->values table graph]
  (-> (insert-into table)
      (values (table->values table graph))))
