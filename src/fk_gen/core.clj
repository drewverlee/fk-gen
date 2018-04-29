(ns fk-gen.core
  (:require [clojure.set :as set]
            [hugsql.core :as hugsql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [honeysql-postgres.format :refer :all]
            [honeysql-postgres.helpers :refer :all]
            [clojure.java.jdbc :as j]
            [table-spec.core :as t]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]))

;; defines get-fk-deps
(hugsql/def-db-fns "fk-deps.sql")




;;TODO not getting foreign key deps
(defn create
  "Returns a vector of insert statements necessary to fulfill all the foreign key constraints of the given table
  table   :keyword : the root of the dependency tree graph you want to generate
  db-info :hashmap : a map describing the database connection information
  "
  [table db-info]
  (let [dfs (fn dfs
              ([n g]
               (dfs [n] #{} g))
              ([nxs v g]
               (let [n (peek nxs)
                     v (conj v n)]
                 (when n (cons n (dfs (filterv #(not (v %)) (concat (pop nxs) (n g))) v g))))))
        keyify (fn [coll] (map #(reduce-kv (fn [m k v] (assoc m k (keyword v))) {} %) coll))
        generate (fn [t] (last (gen/sample (s/gen (keyword (str "table/" (name t)))) 30)))
        create-insert-stmt (fn [table-values {:keys [fk_table fk_column pk_table pk_column]}]
                             (let [select-any {(keyword (str (name fk_table) "/" (name fk_column))) {:select [pk_column] :from [pk_table] :limit 1}}]
                               (-> (insert-into fk_table)
                                   (values [(merge table-values select-any)]))))

        table->fk-deps (group-by :fk_table (keyify (get-fk-deps db-info)))
        fk-deps->graph (fn [table->fk-deps]
                         (->> table->fk-deps
                              (map (fn [[table fk-deps]]
                                     {table (into #{} (map :pk_table fk-deps))}))
                              (apply merge)))
        path->sql (fn [path]
                    (reduce (fn [c t]
                              (conj c
                                    (let [table-values (generate t)]
                                      (if-let [fk-deps (t table->fk-deps)]
                                        (map (fn [fk-table] (create-insert-stmt table-values fk-table))
                                             fk-deps)
                                        (-> (insert-into t)
                                            (values [table-values]))))))
                            [] path))

        db-info->connection-uri (fn [{:keys [subprotocol subname user schema password]}]
                                  {:connection-uri (str "jdbc:" subprotocol ":" subname "?user=" user "&password=" password) :schema schema})]

    (do (-> (db-info->connection-uri db-info)
            (t/tables)
            (t/register))

        (->> table->fk-deps
             fk-deps->graph
             (dfs table)
             path->sql
             reverse))))

