;; ### Overcoming the constraints
;; on the last leg of our journey we need to find a way to actually create sql insert statments.
;; What follows will describe how to do this in general while creating some functions you can use to get
;; you started or serve as a default. We call the function that produces these insert statements: ``table-graph->insert-stmt-plan``

(ns fk-gen.table-graph-to-sql
  "The functions in this namespace can be used to build a
  `table-graph->insert-stmt-plan` function which fulfills its contract of taking a `graph` and `table` and
  returning a vector of honeySQL insert statements. Check the core_test for an example of how this is done."
  (:require [honeysql.helpers :refer [insert-into values]]))


;; One sensible way to implement the table-graph->insert-stmt-plan fn is to generate a sql select statement (honeySQL format)
;; that will be used to create a value that meets the foreign key dependency constraint. This function is used to achieve just that.

(defn ->select-any
  "Returns a vector of select any sql statements"
  [table graph]
  (reduce-kv (fn [values pk-table {:keys [fk-column pk-column]}]
               (assoc values fk-column {:select [pk-column] :from [pk-table] :limit 1}))
             {} (graph table)))

;; Now that you have a way to generate select any statements you still need a way to create the actual sql insert statments.
;; thats where `->insert` comes in. Which is really just a honeySQL code. 

(defn ->insert
  "returns a vector of insert sql statements"
  [table->values table graph]
  (-> (insert-into table)
      (values (table->values table graph))))

;; If your paying attention you have probably noticed we never showed how to generate values that fullfil the data type constraint.
;; Thats because most of that is done by another library called [table-spec](https://github.com/viesti/table-spec).
;; You should read those docs and take away that idea that your going to register your db and produce generators for each table you have.
;; You can use these generators inside your `table-graph->insert-stmt-plan` function to generate values. something like...
;; gen here refers to clojure spec gen

;; <pre><code>
;; (defn gen-value
;;   [t]
;;   (last (gen/sample (s/gen :table/dogs) 30)))
;; </code></pre>

;; which you can use to generate values and then just override them with select any statements when its a foreign key constraint

;; <pre><code>
;; (def table-graph->insert-stmt-plan
;;   (partial ->insert (fn [table graph] [(merge (gen-value table) (->select-any table graph))])))
;; </code></pre>
