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
            [com.rpl.specter :refer [transform MAP-VALS ALL]]
            [clojure.string :as str]))

;; defines function get-fk-deps
(hugsql/def-db-fns "fk-deps.sql")

(defn- fk-deps->graph
  [fk-deps]
  (reduce-kv (fn [c fk v]
               (assoc c fk (reduce
                            (fn [x {:keys [fk-table pk-table fk-column pk-column]}]
                              (assoc x pk-table {:fk-column fk-column :pk-column pk-column}))
                            {} v)))
             {} (group-by :fk-table (transform [ALL MAP-VALS] keyword fk-deps))))

(defn- graph->dfs-path
  ([n f g]
   (graph->dfs-path [n] f #{} g))
  ([nxs f v g]
   (let [n (peek nxs)
         v (conj v n)]
     (when n (cons (f n g) (graph->dfs-path (filterv #(not (v %)) (concat (pop nxs) (keys (g n)))) f v g))))))

(defn- fk-deps->sql-plan
  [{:keys [table table-graph->insert-stmt-plan fk-deps]}]
  (->> fk-deps
       fk-deps->graph
       (graph->dfs-path table table-graph->insert-stmt-plan)
       reverse))

(defn gen
  "Returns a vector of sql insert statement (honeysql format) necessary to fulfill all the foreign key constraints of the given table
  `table`                         :keyword : the name of the table
  `db-info`                       :hashmap : a map describing the database connection information see https://github.com/clojure/java.jdbc.
  `table-graph->insert-stmt-plan` :fn      : a two arity function that takes a table and graph and returns a vector of honeysql formatted insert statements
  "
  [{:keys [db-info table table-graph->insert-stmt-plan]}]
  (fk-deps->sql-plan {:table table :table-graph->insert-stmt-plan table-graph->insert-stmt-plan :fk-deps (get-fk-deps db-info)}))
